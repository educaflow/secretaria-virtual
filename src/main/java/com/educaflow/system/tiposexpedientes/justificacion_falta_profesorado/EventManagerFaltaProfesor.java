package com.educaflow.system.tiposexpedientes.justificacion_falta_profesorado;

import com.axelor.inject.Beans;
import com.axelor.meta.db.MetaFile;
import com.educaflow.base.infrastructure.metafile.MetaFileHelper;
import com.educaflow.base.infrastructure.pdf.CampoFirma;
import com.educaflow.base.infrastructure.pdf.DocumentoPdf;
import com.educaflow.base.infrastructure.pdf.Rectangulo;
import com.educaflow.base.util.SecurityUtil;
import com.educaflow.subsystem.certificados.AlmacenClaveLoader;
import com.educaflow.subsystem.common.db.Persona;
import com.educaflow.subsystem.expedientes.db.TipoResolucionJustificacionFaltaProfesorado;
import com.educaflow.subsystem.expedientes.services.eventmanager.EventContext;
import com.educaflow.subsystem.expedientes.services.eventmanager.OnEnterState;
import com.educaflow.subsystem.expedientes.services.eventmanager.WhenEvent;
import com.educaflow.subsystem.expedientes.db.JustificacionFaltaProfesorado;
import com.educaflow.subsystem.expedientes.db.repo.JustificacionFaltaProfesoradoRepository;
import com.educaflow.base.infrastructure.validation.messages.BusinessException;

import com.educaflow.subsystem.firmas.db.TareaFirma;
import com.educaflow.subsystem.firmas.service.DatosFirma;
import com.educaflow.subsystem.firmas.service.FirmaNotifier;
import com.educaflow.subsystem.firmas.service.FirmaService;
import com.educaflow.subsystem.registroentradasalida.db.RegistroEntrada;
import com.educaflow.subsystem.registroentradasalida.db.RegistroSalida;
import com.educaflow.subsystem.registroentradasalida.db.repo.RegistroEntradaRepository;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;


public class EventManagerFaltaProfesor extends com.educaflow.subsystem.expedientes.services.eventmanager.EventManager<JustificacionFaltaProfesorado, EventManagerFaltaProfesor.State, EventManagerFaltaProfesor.Event,EventManagerFaltaProfesor.Profile> implements FirmaNotifier {

    private final JustificacionFaltaProfesoradoRepository repository;
    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Inject
    RegistroEntradaRepository registroEntradaRepository;
    @Inject
    AlmacenClaveLoader almacenClaveLoader;
    @Inject
    FirmaService firmaService;

    @Inject
    public EventManagerFaltaProfesor(JustificacionFaltaProfesoradoRepository repository) {
        super(JustificacionFaltaProfesorado.class, State.class, Event.class,Profile.class);
        this.repository = repository;
    }

    @Override
    public void triggerInitialEvent(JustificacionFaltaProfesorado justificacionFaltaProfesorado, EventContext<Profile,State> eventContext) throws BusinessException {
        justificacionFaltaProfesorado.setAnyo(LocalDate.now().getYear());
        Persona persona=new Persona();
        persona.setNombre(justificacionFaltaProfesorado.getCreador().getNombre());
        persona.setApellidos(justificacionFaltaProfesorado.getCreador().getApellidos());
        persona.setDni(justificacionFaltaProfesorado.getCreador().getDni());
        justificacionFaltaProfesorado.setPersonaInteresada(persona);
        justificacionFaltaProfesorado.setPersonaSolicitante(persona);
    }


    @WhenEvent
    public void triggerDelete(JustificacionFaltaProfesorado justificacionFaltaProfesorado, JustificacionFaltaProfesorado original, EventContext<Profile,State> eventContext) throws BusinessException {
        //eventContext.updateState(State.);
    }
    @WhenEvent
    public void triggerGuardarDatos(JustificacionFaltaProfesorado justificacionFaltaProfesorado, JustificacionFaltaProfesorado original, EventContext<Profile,State> eventContext) throws BusinessException {
        DocumentoPdf solicitudPdf = justificacionFaltaProfesorado.getDocumentoPdf(JustificacionFaltaProfesorado.TipoDocumentoPdf.SOLICITUD);
        MetaFile pdfSolicitud = MetaFileHelper.createMetaFile(solicitudPdf);
        justificacionFaltaProfesorado.setPdfSolicitud(pdfSolicitud);
        justificacionFaltaProfesorado.setPdfSolicitudFirmado(null);

        eventContext.updateState(State.PENDIENTE_PRESENTACION);


        ///Quitar esto es solo una prueba
        DatosFirma datosFirma=new DatosFirma(SecurityUtil.getUser(),List.of(pdfSolicitud,pdfSolicitud,pdfSolicitud,pdfSolicitud,pdfSolicitud,pdfSolicitud,pdfSolicitud,pdfSolicitud),"Firma Expediente:"+justificacionFaltaProfesorado.getNumeroExpediente(),new Rectangulo(100,100,400,50),this.getClass(),"Datos de callback");
        firmaService.insert(datosFirma);
    }
    @WhenEvent
    public void triggerBack(JustificacionFaltaProfesorado justificacionFaltaProfesorado, JustificacionFaltaProfesorado original, EventContext<Profile,State> eventContext) throws BusinessException {
        State state=State.valueOf(justificacionFaltaProfesorado.getCodeState());

        switch (state) {
            case ENTRADA_DATOS:
                eventContext.updateState( State.ENTRADA_DATOS);
                break;
            case PENDIENTE_PRESENTACION:
                eventContext.updateState( State.ENTRADA_DATOS);
                break;
            case PENDIENTE_RESOLUCION:
                eventContext.updateState( State.ENTRADA_DATOS);
                break;
            case ACEPTADO:
                eventContext.updateState( State.PENDIENTE_PRESENTACION);
                break;
            case RECHAZADO:
                eventContext.updateState( State.PENDIENTE_PRESENTACION);
                break;
            default:
                throw new IllegalArgumentException("State no reconocido: " + state);
        }

    }
    @WhenEvent
    public void triggerPresentar(JustificacionFaltaProfesorado justificacionFaltaProfesorado, JustificacionFaltaProfesorado original, EventContext<Profile,State> eventContext) throws BusinessException {
        RegistroEntrada registroEntrada=eventContext.createRegistroEntrada(justificacionFaltaProfesorado.getPdfSolicitudFirmado(), List.of(justificacionFaltaProfesorado.getJustificante()));
        justificacionFaltaProfesorado.setPdfJustificanteRegistroEntrada(registroEntrada.getDocumentoResguardoPresentacion());
        eventContext.updateState(State.PENDIENTE_RESOLUCION);
        justificacionFaltaProfesorado.setDisconformidad(null);
        justificacionFaltaProfesorado.setResolucion(null);
    }
    @WhenEvent
    public void triggerResolver(JustificacionFaltaProfesorado justificacionFaltaProfesorado, JustificacionFaltaProfesorado original, EventContext<Profile,State> eventContext) throws BusinessException {
        TipoResolucionJustificacionFaltaProfesorado tipoResolucion = justificacionFaltaProfesorado.getTipoResolucion();
        DocumentoPdf resolucion = justificacionFaltaProfesorado.getDocumentoPdf(JustificacionFaltaProfesorado.TipoDocumentoPdf.RESOLUCION);

        DocumentoPdf resolucionFirmada =resolucion.firmar(almacenClaveLoader.getDirector(justificacionFaltaProfesorado.getCentro()),new CampoFirma(new Rectangulo(75,280,400,20)));

        MetaFile pdfResolucion = MetaFileHelper.createMetaFile(resolucionFirmada);

        RegistroSalida registroSalida=eventContext.createRegistroSalida(pdfResolucion, List.of(justificacionFaltaProfesorado.getJustificante()));
        justificacionFaltaProfesorado.setPdfResolucion(registroSalida.getDocumento());
        switch (tipoResolucion) {
            case ACEPTAR:
                eventContext.updateState(State.ACEPTADO);
                break;
            case RECHAZAR:
                eventContext.updateState(State.RECHAZADO);
                break;
            case SUBSANAR_DATOS:
                eventContext.updateState(State.ENTRADA_DATOS);
                break;
            default:
                throw new IllegalArgumentException("Tipo de resolución no reconocido: " + tipoResolucion);
        }
    }


    @Override
    public void notify(TareaFirma tareaFirma, Object callBackData) {
        System.out.println("Notificado!!!!!!:"+callBackData+ " en firma.id="+tareaFirma.getId());
    }

/***************************************************************************************/
/*************************************** Estados ***************************************/
/***************************************************************************************/

    @OnEnterState
    public void onEnterEntradaDatos(JustificacionFaltaProfesorado justificacionFaltaProfesorado, EventContext<Profile,State> eventContext) {

    }
    @OnEnterState
    public void onEnterPendientePresentacion(JustificacionFaltaProfesorado justificacionFaltaProfesorado, EventContext<Profile,State> eventContext) {

    }
    @OnEnterState
    public void onEnterPendienteResolucion(JustificacionFaltaProfesorado justificacionFaltaProfesorado, EventContext<Profile,State> eventContext) {

    }
    @OnEnterState
    public void onEnterAceptado(JustificacionFaltaProfesorado justificacionFaltaProfesorado, EventContext<Profile,State> eventContext) {

    }
    @OnEnterState
    public void onEnterRechazado(JustificacionFaltaProfesorado justificacionFaltaProfesorado, EventContext<Profile,State> eventContext) {

    }




/***************************************************************************************/
/************************** Máquina de Estados del expediente **************************/
/***************************************************************************************/

    //Estados del expediente
    public enum State {
		ENTRADA_DATOS(Profile.CREADOR,true,false,Event.DELETE,Event.GUARDAR_DATOS),
		PENDIENTE_PRESENTACION(Profile.CREADOR,false,false,Event.BACK,Event.PRESENTAR),
		PENDIENTE_RESOLUCION(Profile.RESPONSABLE,false,false,Event.RESOLVER),
		ACEPTADO(Profile.RESPONSABLE,false,true),
		RECHAZADO(Profile.RESPONSABLE,false,true);
                                                        
        private final Profile profile;
        private final java.util.List<Event> events;
        private final boolean initial;
        private final boolean closed;

        // Constructor del enum Estado
        State(Profile profile, boolean initial, boolean closed, Event... events) { // Usamos varargs para los events
            this.profile = profile;
            this.initial=initial;
            this.closed=closed;
            this.events = java.util.Arrays.asList(events); // Convertimos el array de varargs a una List
        }


        public Profile getProfile() {
            return profile;
        }
        public java.util.List<Event> getEvents() {
            return events;
        }
        public boolean isInitial() {
            return initial;
        }
        public boolean isClosed() {
            return closed;
        }


    }

    public enum Event {
        DELETE,
        GUARDAR_DATOS,
        BACK,
        PRESENTAR,
        RESOLVER
    }

    public enum Profile {
        CREADOR,
        RESPONSABLE
    }

}