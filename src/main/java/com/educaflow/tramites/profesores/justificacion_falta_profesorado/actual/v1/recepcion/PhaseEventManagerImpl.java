package com.educaflow.tramites.profesores.justificacion_falta_profesorado.actual.v1.recepcion;

import com.axelor.meta.db.MetaFile;
import com.educaflow.base.infrastructure.metafile.MetaFileHelper;
import com.educaflow.base.infrastructure.pdf.DocumentoPdf;
import com.educaflow.subsystem.expedientes.services.eventmanager.EventContext;
import com.educaflow.subsystem.expedientes.services.eventmanager.OnEnterState;
import com.educaflow.subsystem.expedientes.services.eventmanager.State;
import com.educaflow.subsystem.expedientes.services.eventmanager.WhenEvent;
import com.educaflow.subsystem.expedientes.db.JustificacionFaltaProfesoradoV1;
import com.educaflow.subsystem.expedientes.db.repo.JustificacionFaltaProfesoradoV1Repository;
import com.educaflow.base.infrastructure.validation.messages.BusinessException;
import com.educaflow.tramites.profesores.justificacion_falta_profesorado.actual.v1.States;

import com.educaflow.subsystem.firmas.db.TareaFirma;
import com.educaflow.subsystem.firmas.service.TareaFirmaNotifier;
import com.educaflow.subsystem.registroentradasalida.db.RegistroEntrada;
import com.educaflow.subsystem.registroentradasalida.db.repo.RegistroEntradaRepository;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


public class PhaseEventManagerImpl extends com.educaflow.subsystem.expedientes.services.eventmanager.PhaseEventManager<JustificacionFaltaProfesoradoV1> implements TareaFirmaNotifier {

    private final JustificacionFaltaProfesoradoV1Repository repository;
    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Inject
    RegistroEntradaRepository registroEntradaRepository;

    @Inject
    public PhaseEventManagerImpl(JustificacionFaltaProfesoradoV1Repository repository) {
        super(JustificacionFaltaProfesoradoV1.class);
        this.repository = repository;
    }



    @WhenEvent
    public void triggerDelete(JustificacionFaltaProfesoradoV1 justificacionFaltaProfesorado, JustificacionFaltaProfesoradoV1 original, EventContext eventContext) throws BusinessException {
        //eventContext.updateState(States.Recepcion.);
    }
    @WhenEvent
    public void triggerGuardarDatos(JustificacionFaltaProfesoradoV1 justificacionFaltaProfesorado, JustificacionFaltaProfesoradoV1 original, EventContext eventContext) throws BusinessException {
        DocumentoPdf solicitudPdf = justificacionFaltaProfesorado.getDocumentoPdf(JustificacionFaltaProfesoradoV1.TipoDocumentoPdf.SOLICITUD);
        MetaFile pdfSolicitud = MetaFileHelper.createMetaFile(solicitudPdf);
        justificacionFaltaProfesorado.setPdfSolicitud(pdfSolicitud);

        eventContext.updateState(States.Recepcion.PENDIENTE_PRESENTACION);
    }
    @WhenEvent
    public void triggerBack(JustificacionFaltaProfesoradoV1 justificacionFaltaProfesorado, JustificacionFaltaProfesoradoV1 original, EventContext eventContext) throws BusinessException {
        //El codeState ya no lleva la fase, así que el estado se resuelve con la pareja de columnas.
        //Como aquí el tipo de expediente se conoce en compilación, se pregunta directamente a su
        //States.INSTANCE en vez de pasar por la entidad.
        State state = States.INSTANCE
                .getState(justificacionFaltaProfesorado.getCodePhase(), justificacionFaltaProfesorado.getCodeState())
                .orElseThrow(() -> new IllegalArgumentException("State no reconocido: "
                        + justificacionFaltaProfesorado.getCodePhase() + "/" + justificacionFaltaProfesorado.getCodeState()));

        //Solo los estados de esta fase: el evento lo atiende el PhaseEventManager de la fase en la que
        //está el expediente, así que aquí nunca llega un estado de TRAMITACION.
        switch (state) {
            case States.Recepcion.PENDIENTE_PRESENTACION:
                eventContext.updateState( States.Recepcion.ENTRADA_DATOS);
                break;
            default:
                throw new IllegalArgumentException("State no reconocido: " + state);
        }

    }
    @WhenEvent
    public void triggerPresentar(JustificacionFaltaProfesoradoV1 exp, JustificacionFaltaProfesoradoV1 original, EventContext eventContext) throws BusinessException {
        RegistroEntrada registroEntrada = eventContext.createRegistroEntrada(exp.getPdfSolicitudFirmado(), List.of(exp.getJustificante()));
        exp.setPdfJustificanteRegistroEntrada(registroEntrada.getDocumentoResguardoPresentacion());
        eventContext.updateState(States.Tramitacion.PENDIENTE_RESOLUCION);
        exp.setDisconformidad(null);
        exp.setResolucion(null);
    }


    @Override
    public void notify(TareaFirma tareaFirma, Object callBackData) {
        //System.out.println("Notificado!!!!!!:"+callBackData+ " en firma.id="+tareaFirma.getId());
    }

/***************************************************************************************/
/*************************************** Estados ***************************************/
/***************************************************************************************/

    @OnEnterState
    public void onEnterEntradaDatos(JustificacionFaltaProfesoradoV1 justificacionFaltaProfesorado, EventContext eventContext) {

    }
    @OnEnterState
    public void onEnterPendientePresentacion(JustificacionFaltaProfesoradoV1 justificacionFaltaProfesorado, EventContext eventContext) {

    }

}
