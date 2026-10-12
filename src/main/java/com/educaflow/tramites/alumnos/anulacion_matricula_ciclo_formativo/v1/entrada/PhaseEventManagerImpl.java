package com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1.entrada;

import com.educaflow.base.infrastructure.metafile.MetaFileHelper;
import com.educaflow.base.infrastructure.pdf.DocumentoPdf;
import com.educaflow.base.infrastructure.validation.messages.BusinessException;
import com.educaflow.base.util.SecurityUtil;
import com.educaflow.base.util.Convert;
import com.educaflow.subsystem.expedientes.db.AnulacionMatriculaCicloFormativoV1;
import com.educaflow.subsystem.expedientes.db.repo.AnulacionMatriculaCicloFormativoV1Repository;
import com.educaflow.subsystem.tramitador.tramitacion.eventmanager.EventContext;
import com.educaflow.subsystem.tramitador.tramitacion.eventmanager.OnEnterState;
import com.educaflow.subsystem.tramitador.tramitacion.eventmanager.PhaseEventManager;
import com.educaflow.subsystem.tramitador.tramitacion.eventmanager.WhenEvent;
import com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1.AnulacionMatriculaCicloFormativoV1Util;
import com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1.States;
import com.educaflow.tramites.shared.firma.FirmaExpedienteHelper;
import com.educaflow.subsystem.tramitador.tramitacion.util.ExpedienteUtil;
import com.educaflow.tramites.shared.registro.RegistroEntradaSalidaExpedienteHelper;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;


public class PhaseEventManagerImpl extends PhaseEventManager<AnulacionMatriculaCicloFormativoV1> {

    // El campoFirma del hueco de la firma en documentospdf/solicitud.xml: el mismo nombre que la
    // <action-method> de AutoFirma de esta fase pasa a firmarDocumentoEnCampo.
    private static final String CAMPO_FIRMA_SOLICITUD = "firmaSolicitante";

    private final AnulacionMatriculaCicloFormativoV1Repository repository;
    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Inject
    RegistroEntradaSalidaExpedienteHelper registroEntradaSalidaExpedienteHelper;

    @Inject
    FirmaExpedienteHelper firmaExpedienteHelper;

    @Inject
    public PhaseEventManagerImpl(AnulacionMatriculaCicloFormativoV1Repository repository) {
        super(AnulacionMatriculaCicloFormativoV1.class);
        this.repository = repository;
    }


    @WhenEvent
    public void triggerDelete(AnulacionMatriculaCicloFormativoV1 expediente, AnulacionMatriculaCicloFormativoV1 original, EventContext eventContext) throws BusinessException {
        AnulacionMatriculaCicloFormativoV1Util.exigeSerElCreador(expediente, "Solo puede borrar sus propias solicitudes");
    }

    /** Solo en papel: adjuntada la solicitud escaneada, se pasa a copiar sus datos. */
    @WhenEvent
    public void triggerContinuar(AnulacionMatriculaCicloFormativoV1 expediente, AnulacionMatriculaCicloFormativoV1 original, EventContext eventContext) throws BusinessException {
        AnulacionMatriculaCicloFormativoV1Util.exigeSerElCreador(expediente, "Solo puede modificar sus propias solicitudes");
        Preconditions.checkState(Boolean.TRUE.equals(original.getPresentadoEnPapel()), "El expediente %s (presentadoEnPapel=%s) no puede disparar este evento desde el estado '%s/%s'.", original.getNumeroExpediente(), original.getPresentadoEnPapel(), original.getCodePhase(), original.getCodeState());
        // El escaneado lo subió el usuario: la fecha de la solicitud firmada a mano es la de la subida.
        expediente.setFechaHoraPdfSolicitudFirmada(expediente.getPdfSolicitudFirmada().getCreatedOn());

        eventContext.updateState(States.Entrada.ENTRADA_DATOS);
    }

    /**
     * Genera la solicitud con los datos tecleados. Telemáticamente el alumno tiene que firmarla; en papel ya la
     * firmó a mano la persona que la entregó, así que se presenta con el escaneado.
     */
    @WhenEvent
    public void triggerGuardarDatos(AnulacionMatriculaCicloFormativoV1 expediente, AnulacionMatriculaCicloFormativoV1 original, EventContext eventContext) throws BusinessException {
        AnulacionMatriculaCicloFormativoV1Util.exigeSerElCreador(expediente, "Solo puede modificar sus propias solicitudes");

        DocumentoPdf solicitudPdf = expediente.getDocumentoPdf(AnulacionMatriculaCicloFormativoV1.TipoDocumentoPdf.SOLICITUD);
        expediente.setPdfSolicitud(MetaFileHelper.createMetaFile(solicitudPdf));
        expediente.setFechaHoraPdfSolicitud(LocalDateTime.now(Convert.defaultZoneId));

        if (Boolean.TRUE.equals(original.getPresentadoEnPapel())) {
            registroEntradaSalidaExpedienteHelper.registroEntradaSolicitud(expediente, List.of(), eventContext);
            eventContext.updateState(States.Verificacion.PENDIENTE_VERIFICACION);
        } else {
            expediente.setPdfSolicitudFirmada(null);
            expediente.setFechaHoraPdfSolicitudFirmada(null);
            eventContext.updateState(States.Entrada.PENDIENTE_PRESENTACION);
        }
    }

    /**
     * Lo disparan dos estados: PENDIENTE_PRESENTACION vuelve a los datos, y ENTRADA_DATOS (solo en papel) vuelve
     * al escaneado para cambiarlo.
     */
    @WhenEvent
    public void triggerBack(AnulacionMatriculaCicloFormativoV1 expediente, AnulacionMatriculaCicloFormativoV1 original, EventContext eventContext) throws BusinessException {
        expediente.setClaveCertificado(null);

        AnulacionMatriculaCicloFormativoV1Util.exigeSerElCreador(expediente, "Solo puede volver atrás en sus propias solicitudes");

        if (ExpedienteUtil.getState(original) == States.Entrada.ENTRADA_DATOS) {
            Preconditions.checkState(Boolean.TRUE.equals(original.getPresentadoEnPapel()), "El expediente %s (presentadoEnPapel=%s) no puede disparar este evento desde el estado '%s/%s'.", original.getNumeroExpediente(), original.getPresentadoEnPapel(), original.getCodePhase(), original.getCodeState());
            eventContext.updateState(States.Entrada.PENDIENTE_DOCUMENTO_ESCANEADO);
        } else {
            eventContext.updateState(States.Entrada.ENTRADA_DATOS);
        }
    }

    /** Solo telemáticamente: el alumno firma la solicitud generada. En papel se presenta en GUARDAR_DATOS. */
    @WhenEvent
    public void triggerPresentar(AnulacionMatriculaCicloFormativoV1 expediente, AnulacionMatriculaCicloFormativoV1 original, EventContext eventContext) throws BusinessException {
        try {
            AnulacionMatriculaCicloFormativoV1Util.exigeSerElCreador(expediente, "Solo puede presentar sus propias solicitudes");
            Preconditions.checkState(Boolean.TRUE.equals(original.getPresentadoEnPapel()) == false, "El expediente %s (presentadoEnPapel=%s) no puede disparar este evento desde el estado '%s/%s'.", original.getNumeroExpediente(), original.getPresentadoEnPapel(), original.getCodePhase(), original.getCodeState());
            firmaExpedienteHelper.firmarSolicitudEnServidor(SecurityUtil.getUser().getDni(), expediente, CAMPO_FIRMA_SOLICITUD);
            firmaExpedienteHelper.actualizarFechaHoraPdfSolicitudFirmada(expediente);
            registroEntradaSalidaExpedienteHelper.registroEntradaSolicitud(expediente, List.of(), eventContext);
            eventContext.updateState(States.Verificacion.PENDIENTE_VERIFICACION);
        } finally {
            expediente.setClaveCertificado(null);
        }
    }


/***************************************************************************************/
/*************************************** Estados ***************************************/
/***************************************************************************************/

    @OnEnterState
    public void onEnterPendienteDocumentoEscaneado(AnulacionMatriculaCicloFormativoV1 expediente, EventContext eventContext) {

    }
    @OnEnterState
    public void onEnterEntradaDatos(AnulacionMatriculaCicloFormativoV1 expediente, EventContext eventContext) {

    }
    @OnEnterState
    public void onEnterPendientePresentacion(AnulacionMatriculaCicloFormativoV1 expediente, EventContext eventContext) {

    }

}
