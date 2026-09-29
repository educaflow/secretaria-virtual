package com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1.revision;

import com.educaflow.base.infrastructure.metafile.MetaFileHelper;
import com.educaflow.base.infrastructure.pdf.DocumentoPdf;
import com.educaflow.base.infrastructure.validation.messages.BusinessException;
import com.educaflow.base.util.Convert;
import com.educaflow.base.util.SecurityUtil;
import com.educaflow.subsystem.expedientes.db.AnulacionMatriculaCicloFormativoV1;
import com.educaflow.subsystem.expedientes.db.SentidoRevisionAnulacionMatriculaCicloFormativoV1;
import com.educaflow.subsystem.expedientes.db.repo.AnulacionMatriculaCicloFormativoV1Repository;
import com.educaflow.subsystem.tramitador.tramitacion.eventmanager.EventContext;
import com.educaflow.subsystem.tramitador.tramitacion.eventmanager.OnEnterState;
import com.educaflow.subsystem.tramitador.tramitacion.eventmanager.PhaseEventManager;
import com.educaflow.subsystem.tramitador.tramitacion.eventmanager.WhenEvent;
import com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1.AnulacionMatriculaCicloFormativoV1Util;
import com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1.States;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;


public class PhaseEventManagerImpl extends PhaseEventManager<AnulacionMatriculaCicloFormativoV1> {

    private final AnulacionMatriculaCicloFormativoV1Repository repository;
    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Inject
    public PhaseEventManagerImpl(AnulacionMatriculaCicloFormativoV1Repository repository) {
        super(AnulacionMatriculaCicloFormativoV1.class);
        this.repository = repository;
    }


    @WhenEvent
    public void triggerContinuar(AnulacionMatriculaCicloFormativoV1 expediente, AnulacionMatriculaCicloFormativoV1 original, EventContext eventContext) throws BusinessException {
        AnulacionMatriculaCicloFormativoV1Util.exigePertenecerAlCentroDelExpediente(expediente, "Solo puede revisar solicitudes de su propio centro");
        AnulacionMatriculaCicloFormativoV1Util.exigeOstentarElPerfilDelEstado(expediente, "Solo la secretaría del centro puede revisar esta solicitud");

        expediente.setFechaRevision(LocalDate.now(Convert.defaultZoneId));
        expediente.setRevisadoPor(SecurityUtil.getUser());

        SentidoRevisionAnulacionMatriculaCicloFormativoV1 sentidoRevision = expediente.getSentidoRevision();
        switch (sentidoRevision) {
            case ACEPTAR, RECHAZAR -> enviarALaFirmaDelDirector(expediente, eventContext);
            case SUBSANAR -> devolverAlAlumnoParaQueSubsane(expediente, eventContext);
            case null, default -> throw new IllegalArgumentException("Sentido de la revisión no reconocido: " + sentidoRevision);
        }
    }

    private void enviarALaFirmaDelDirector(AnulacionMatriculaCicloFormativoV1 expediente, EventContext eventContext) {
        if (expediente.getSentidoRevision() != SentidoRevisionAnulacionMatriculaCicloFormativoV1.RECHAZAR) {
            expediente.setMotivoRechazo(null);
        }
        expediente.setTextoSubsanacion(null);
        AnulacionMatriculaCicloFormativoV1Util.borrarDevolucionDelDirector(expediente);

        expediente.setFechaResolucion(LocalDate.now(Convert.defaultZoneId));

        DocumentoPdf resolucionPdf = expediente.getDocumentoPdf(AnulacionMatriculaCicloFormativoV1.TipoDocumentoPdf.RESOLUCION);
        expediente.setPdfResolucion(MetaFileHelper.createMetaFile(resolucionPdf));

        eventContext.updateState(States.Resolucion.PENDIENTE_FIRMA_DIRECTOR);
    }

    private void devolverAlAlumnoParaQueSubsane(AnulacionMatriculaCicloFormativoV1 expediente, EventContext eventContext) {
        expediente.setMotivoRechazo(null);
        AnulacionMatriculaCicloFormativoV1Util.borrarDevolucionDelDirector(expediente);
        expediente.setPdfResolucion(null);

        // En papel la persona entregará una solicitud nueva: se vuelve a empezar por el escaneado.
        if (Boolean.TRUE.equals(expediente.getPresentadoEnPapel())) {
            eventContext.updateState(States.Solicitud.PENDIENTE_DOCUMENTO_ESCANEADO);
        } else {
            eventContext.updateState(States.Solicitud.DATOS_SOLICITUD);
        }
    }


/***************************************************************************************/
/*************************************** Estados ***************************************/
/***************************************************************************************/

    @OnEnterState
    public void onEnterPendienteRevision(AnulacionMatriculaCicloFormativoV1 expediente, EventContext eventContext) {

    }

}
