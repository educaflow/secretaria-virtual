package com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1.entrada;

import com.educaflow.base.infrastructure.metafile.MetaFileHelper;
import com.educaflow.base.infrastructure.pdf.DocumentoPdf;
import com.educaflow.base.infrastructure.validation.messages.BusinessException;
import com.educaflow.base.util.Convert;
import com.educaflow.subsystem.expedientes.db.AnulacionMatriculaCicloFormativoV1;
import com.educaflow.subsystem.expedientes.db.repo.AnulacionMatriculaCicloFormativoV1Repository;
import com.educaflow.subsystem.tramitador.tramitacion.eventmanager.EventContext;
import com.educaflow.subsystem.tramitador.tramitacion.eventmanager.OnEnterState;
import com.educaflow.subsystem.tramitador.tramitacion.eventmanager.PhaseEventManager;
import com.educaflow.subsystem.tramitador.tramitacion.eventmanager.WhenEvent;
import com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1.AnulacionMatriculaCicloFormativoV1Util;
import com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1.States;
import com.educaflow.tramites.util.entrada.EntradaHelper;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;


public class PhaseEventManagerImpl extends PhaseEventManager<AnulacionMatriculaCicloFormativoV1> {

    // El campoFirma del hueco de la firma en documentospdf/solicitud.xml: el mismo nombre que la
    // <action-method> de AutoFirma de esta fase pasa a firmarDocumentoEnCampo.
    private static final String CAMPO_FIRMA_SOLICITUD = "firmaSolicitante";

    private final AnulacionMatriculaCicloFormativoV1Repository repository;
    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Inject
    EntradaHelper entradaHelper;

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
        EntradaHelper.exigePresentadoEnPapel(original, true);

        eventContext.updateState(States.Entrada.ENTRADA_DATOS);
    }

    /**
     * Genera la solicitud con los datos tecleados. Telemáticamente el alumno tiene que firmarla; en papel ya la
     * firmó a mano la persona que la entregó, así que se presenta con el escaneado.
     */
    @WhenEvent
    public void triggerGuardarDatos(AnulacionMatriculaCicloFormativoV1 expediente, AnulacionMatriculaCicloFormativoV1 original, EventContext eventContext) throws BusinessException {
        AnulacionMatriculaCicloFormativoV1Util.exigeSerElCreador(expediente, "Solo puede modificar sus propias solicitudes");

        expediente.setFechaSolicitud(LocalDate.now(Convert.defaultZoneId));

        DocumentoPdf solicitudPdf = expediente.getDocumentoPdf(AnulacionMatriculaCicloFormativoV1.TipoDocumentoPdf.SOLICITUD);
        expediente.setPdfSolicitud(MetaFileHelper.createMetaFile(solicitudPdf));

        if (Boolean.TRUE.equals(original.getPresentadoEnPapel())) {
            presentar(expediente, eventContext);
        } else {
            expediente.setPdfSolicitudFirmada(null);
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

        if (EntradaHelper.estaEn(original, States.Entrada.ENTRADA_DATOS)) {
            EntradaHelper.exigePresentadoEnPapel(original, true);
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
            EntradaHelper.exigePresentadoEnPapel(original, false);
            entradaHelper.firmarSolicitudSiEsEnServidor(expediente, AnulacionMatriculaCicloFormativoV1Util.CAMPOS_ENTRADA, CAMPO_FIRMA_SOLICITUD);

            presentar(expediente, eventContext);
        } finally {
            expediente.setClaveCertificado(null);
        }
    }

    /**
     * Registra la entrada de la solicitud que hay en pdfSolicitudFirmada (la firmada por el alumno o, en papel, la
     * escaneada) y la pasa a verificación.
     */
    private void presentar(AnulacionMatriculaCicloFormativoV1 expediente, EventContext eventContext) {
        entradaHelper.presentar(expediente, AnulacionMatriculaCicloFormativoV1Util.CAMPOS_ENTRADA, List.of(), eventContext);

        // La anulación surte efecto desde esta fecha, y la resolución la imprime.
        expediente.setFechaHoraPresentacion(LocalDateTime.now(Convert.defaultZoneId));

        eventContext.updateState(States.Verificacion.PENDIENTE_VERIFICACION);
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
