package com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1.solicitud;

import com.educaflow.base.infrastructure.metafile.MetaFileHelper;
import com.educaflow.base.infrastructure.pdf.DocumentoPdf;
import com.educaflow.base.infrastructure.pdf.Rectangulo;
import com.educaflow.base.infrastructure.validation.messages.BusinessException;
import com.educaflow.base.util.Convert;
import com.educaflow.base.util.SecurityUtil;
import com.educaflow.subsystem.criptografia.service.SituacionFirma;
import com.educaflow.subsystem.criptografia.util.CertificadoDigitalHelper;
import com.educaflow.subsystem.expedientes.db.AnulacionMatriculaCicloFormativoV1;
import com.educaflow.subsystem.expedientes.db.repo.AnulacionMatriculaCicloFormativoV1Repository;
import com.educaflow.subsystem.expedientes.services.eventmanager.EventContext;
import com.educaflow.subsystem.expedientes.services.eventmanager.OnEnterState;
import com.educaflow.subsystem.expedientes.services.eventmanager.WhenEvent;
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


public class PhaseEventManagerImpl extends com.educaflow.subsystem.expedientes.services.eventmanager.PhaseEventManager<AnulacionMatriculaCicloFormativoV1> {

    // Hueco de la firma del alumno en la solicitud, medido sobre el PDF que genera el documento.
    private static final Rectangulo POSICION_FIRMA_SOLICITUD = new Rectangulo(320, 418, 240, 32);

    private static final int PAGINA_FIRMA_SOLICITUD = 1;

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

    @WhenEvent
    public void triggerContinuar(AnulacionMatriculaCicloFormativoV1 expediente, AnulacionMatriculaCicloFormativoV1 original, EventContext eventContext) throws BusinessException {
        AnulacionMatriculaCicloFormativoV1Util.exigeSerElCreador(expediente, "Solo puede modificar sus propias solicitudes");

        expediente.setFechaSolicitud(LocalDate.now(Convert.defaultZoneId));

        DocumentoPdf solicitudPdf = expediente.getDocumentoPdf(AnulacionMatriculaCicloFormativoV1.TipoDocumentoPdf.SOLICITUD);
        expediente.setPdfSolicitud(MetaFileHelper.createMetaFile(solicitudPdf));
        expediente.setPdfSolicitudFirmada(null);

        eventContext.updateState(States.Solicitud.PENDIENTE_FIRMA);
    }

    @WhenEvent
    public void triggerVolver(AnulacionMatriculaCicloFormativoV1 expediente, AnulacionMatriculaCicloFormativoV1 original, EventContext eventContext) throws BusinessException {
        AnulacionMatriculaCicloFormativoV1Util.exigeSerElCreador(expediente, "Solo puede volver atrás en sus propias solicitudes");

        expediente.setClaveCertificado(null);

        eventContext.updateState(States.Solicitud.DATOS_SOLICITUD);
    }

    @WhenEvent
    public void triggerPresentar(AnulacionMatriculaCicloFormativoV1 expediente, AnulacionMatriculaCicloFormativoV1 original, EventContext eventContext) throws BusinessException {
        AnulacionMatriculaCicloFormativoV1Util.exigeSerElCreador(expediente, "Solo puede presentar sus propias solicitudes");

        String dniFirmante = SecurityUtil.getUser().getDni();
        SituacionFirma situacionFirma = CertificadoDigitalHelper.getSituacionFirmaByDni(dniFirmante);

        try {
            if (situacionFirma.isFirmaEnServidor()) {
                expediente.setPdfSolicitudFirmada(firmaServidorHelper.firmarEnServidor(
                        dniFirmante,
                        situacionFirma,
                        expediente.getClaveCertificado(),
                        expediente.getPdfSolicitud(),
                        POSICION_FIRMA_SOLICITUD,
                        PAGINA_FIRMA_SOLICITUD));
            }

            RegistroEntrada registroEntrada = eventContext.createRegistroEntrada(expediente.getPdfSolicitudFirmada(), List.of());
            expediente.setPdfJustificanteRegistroEntrada(registroEntrada.getDocumentoResguardoPresentacion());

            expediente.setFechaHoraPresentacion(LocalDateTime.now(Convert.defaultZoneId));

            expediente.setTextoSubsanacion(null);
            expediente.setSentidoRevision(null);
            expediente.setMotivoRechazo(null);
            AnulacionMatriculaCicloFormativoV1Util.borrarDevolucionDelDirector(expediente);

            eventContext.updateState(States.Revision.PENDIENTE_REVISION);
        } finally {
            expediente.setClaveCertificado(null);
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

}
