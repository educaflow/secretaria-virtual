package com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1.solicitud;

import com.educaflow.base.infrastructure.metafile.MetaFileHelper;
import com.educaflow.base.infrastructure.pdf.DocumentoPdf;
import com.educaflow.base.infrastructure.validation.messages.BusinessException;
import com.educaflow.base.util.Convert;
import com.educaflow.base.util.SecurityUtil;
import com.educaflow.subsystem.criptografia.service.SituacionFirma;
import com.educaflow.subsystem.criptografia.util.CertificadoDigitalHelper;
import com.educaflow.subsystem.expedientes.db.AnulacionMatriculaCicloFormativoV1;
import com.educaflow.subsystem.expedientes.db.repo.AnulacionMatriculaCicloFormativoV1Repository;
import com.educaflow.subsystem.tramitador.tramitacion.eventmanager.EventContext;
import com.educaflow.subsystem.tramitador.tramitacion.eventmanager.OnEnterState;
import com.educaflow.subsystem.tramitador.tramitacion.eventmanager.PhaseEventManager;
import com.educaflow.subsystem.tramitador.tramitacion.eventmanager.WhenEvent;
import com.educaflow.subsystem.registroentradasalida.db.RegistroEntrada;
import com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1.AnulacionMatriculaCicloFormativoV1Util;
import com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1.States;
import com.educaflow.tramites.util.firma.FirmaServidorHelper;

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
    FirmaServidorHelper firmaServidorHelper;

    @Inject
    public PhaseEventManagerImpl(AnulacionMatriculaCicloFormativoV1Repository repository) {
        super(AnulacionMatriculaCicloFormativoV1.class);
        this.repository = repository;
    }


    @WhenEvent
    public void triggerDelete(AnulacionMatriculaCicloFormativoV1 expediente, AnulacionMatriculaCicloFormativoV1 original, EventContext eventContext) throws BusinessException {
        AnulacionMatriculaCicloFormativoV1Util.exigeSerElCreador(expediente, "Solo puede borrar sus propias solicitudes");
    }

    /**
     * Lo disparan dos estados. En PENDIENTE_DOCUMENTO_ESCANEADO (solo en papel) se pasa a copiar los datos de la
     * solicitud adjuntada. En DATOS_SOLICITUD se genera la solicitud: si no es en papel, el alumno tiene que
     * firmarla; en papel ya la firmó a mano la persona que la entregó, así que se presenta con el escaneado.
     */
    @WhenEvent
    public void triggerContinuar(AnulacionMatriculaCicloFormativoV1 expediente, AnulacionMatriculaCicloFormativoV1 original, EventContext eventContext) throws BusinessException {
        AnulacionMatriculaCicloFormativoV1Util.exigeSerElCreador(expediente, "Solo puede modificar sus propias solicitudes");

        if (esEstado(original, States.Solicitud.PENDIENTE_DOCUMENTO_ESCANEADO)) {
            exigeModo(original, true);
            eventContext.updateState(States.Solicitud.DATOS_SOLICITUD);
            return;
        }

        expediente.setFechaSolicitud(LocalDate.now(Convert.defaultZoneId));

        DocumentoPdf solicitudPdf = expediente.getDocumentoPdf(AnulacionMatriculaCicloFormativoV1.TipoDocumentoPdf.SOLICITUD);
        expediente.setPdfSolicitud(MetaFileHelper.createMetaFile(solicitudPdf));

        if (Boolean.TRUE.equals(expediente.getPresentadoEnPapel())) {
            presentar(expediente, eventContext);
        } else {
            expediente.setPdfSolicitudFirmada(null);
            eventContext.updateState(States.Solicitud.PENDIENTE_FIRMA);
        }
    }

    /**
     * Lo disparan dos estados: PENDIENTE_FIRMA vuelve a los datos, y DATOS_SOLICITUD (solo en papel) vuelve al
     * escaneado para cambiarlo.
     */
    @WhenEvent
    public void triggerVolver(AnulacionMatriculaCicloFormativoV1 expediente, AnulacionMatriculaCicloFormativoV1 original, EventContext eventContext) throws BusinessException {
        AnulacionMatriculaCicloFormativoV1Util.exigeSerElCreador(expediente, "Solo puede volver atrás en sus propias solicitudes");

        if (esEstado(original, States.Solicitud.DATOS_SOLICITUD)) {
            exigeModo(original, true);
            eventContext.updateState(States.Solicitud.PENDIENTE_DOCUMENTO_ESCANEADO);
            return;
        }

        expediente.setClaveCertificado(null);

        eventContext.updateState(States.Solicitud.DATOS_SOLICITUD);
    }

    /** Solo fuera de papel: el alumno firma la solicitud generada. En papel se presenta en el CONTINUAR de los datos. */
    @WhenEvent
    public void triggerPresentar(AnulacionMatriculaCicloFormativoV1 expediente, AnulacionMatriculaCicloFormativoV1 original, EventContext eventContext) throws BusinessException {
        AnulacionMatriculaCicloFormativoV1Util.exigeSerElCreador(expediente, "Solo puede presentar sus propias solicitudes");

        try {
            exigeModo(original, false);
            firmarSolicitudSiEsEnServidor(expediente);

            presentar(expediente, eventContext);
        } finally {
            expediente.setClaveCertificado(null);
        }
    }

    /**
     * Registra la entrada de la solicitud que hay en pdfSolicitudFirmada (la firmada por el alumno o, en papel, la
     * escaneada) y la pasa a revisión.
     */
    private static void presentar(AnulacionMatriculaCicloFormativoV1 expediente, EventContext eventContext) {
        RegistroEntrada registroEntrada = eventContext.createRegistroEntrada(expediente.getPdfSolicitudFirmada(), List.of());
        expediente.setPdfJustificanteRegistroEntrada(registroEntrada.getDocumentoResguardoPresentacion());

        expediente.setFechaHoraPresentacion(LocalDateTime.now(Convert.defaultZoneId));

        expediente.setTextoSubsanacion(null);
        expediente.setSentidoRevision(null);
        expediente.setMotivoRechazo(null);
        AnulacionMatriculaCicloFormativoV1Util.borrarDevolucionDelDirector(expediente);

        eventContext.updateState(States.Revision.PENDIENTE_REVISION);
    }


    private void firmarSolicitudSiEsEnServidor(AnulacionMatriculaCicloFormativoV1 expediente) throws BusinessException {
        String dniFirmante = SecurityUtil.getUser().getDni();
        SituacionFirma situacionFirma = CertificadoDigitalHelper.getSituacionFirmaByDni(dniFirmante);

        if (situacionFirma.isFirmaEnServidor()) {
            expediente.setPdfSolicitudFirmada(firmaServidorHelper.firmarEnServidor(
                    dniFirmante,
                    situacionFirma,
                    expediente.getClaveCertificado(),
                    expediente.getPdfSolicitud(),
                    CAMPO_FIRMA_SOLICITUD));
        }
    }

    private static boolean esEstado(AnulacionMatriculaCicloFormativoV1 expediente, States.Solicitud estado) {
        return estado.getPhase().getCode().equals(expediente.getCodePhase()) && estado.getCode().equals(expediente.getCodeState());
    }

    /**
     * El camino en papel y el de firma comparten estados pero no eventos: la vista solo ofrece a cada modo los
     * suyos, así que si llega el del otro modo se registraría sin firmar una solicitud que había que firmar, o se
     * pediría firmar a quien solo la registra.
     */
    private static void exigeModo(AnulacionMatriculaCicloFormativoV1 expediente, boolean presentadoEnPapel) {
        if (Boolean.TRUE.equals(expediente.getPresentadoEnPapel()) != presentadoEnPapel) {
            throw new IllegalStateException("El expediente " + expediente.getNumeroExpediente() + " (presentadoEnPapel="
                    + expediente.getPresentadoEnPapel() + ") no puede disparar este evento desde el estado '"
                    + expediente.getCodePhase() + "/" + expediente.getCodeState() + "'.");
        }
    }

/***************************************************************************************/
/*************************************** Estados ***************************************/
/***************************************************************************************/

    @OnEnterState
    public void onEnterDatosSolicitud(AnulacionMatriculaCicloFormativoV1 expediente, EventContext eventContext) {

    }
    @OnEnterState
    public void onEnterPendienteFirma(AnulacionMatriculaCicloFormativoV1 expediente, EventContext eventContext) {

    }
    @OnEnterState
    public void onEnterPendienteDocumentoEscaneado(AnulacionMatriculaCicloFormativoV1 expediente, EventContext eventContext) {

    }

}
