package com.educaflow.tramites.prueba.tiposexpedientes.comision_servicio;

import com.axelor.inject.Beans;
import com.educaflow.subsystem.expedientes.services.eventmanager.EventContext;
import com.educaflow.subsystem.expedientes.services.eventmanager.OnEnterState;
import com.educaflow.subsystem.expedientes.services.eventmanager.WhenEvent;
import com.educaflow.subsystem.expedientes.db.ComisionServicio;
import com.educaflow.subsystem.expedientes.db.repo.ComisionServicioRepository;
import com.educaflow.base.infrastructure.validation.messages.BusinessException;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class EventManagerImpl extends com.educaflow.subsystem.expedientes.services.eventmanager.EventManager<ComisionServicio, EventManagerImpl.State, EventManagerImpl.Event,EventManagerImpl.Profile> {

    private final ComisionServicioRepository repository;
    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Inject
    public EventManagerImpl(ComisionServicioRepository repository) {
        super(ComisionServicio.class, State.class, Event.class,Profile.class);
        this.repository = repository;
    }

    @Override
    public void triggerInitialEvent(ComisionServicio comisionServicio, EventContext<Profile,State> eventContext) throws BusinessException {


    }


    @WhenEvent
    public void triggerDelete(ComisionServicio comisionServicio, ComisionServicio original, EventContext<Profile,State> eventContext) throws BusinessException {
        //eventContext.updateState(State.);
    }
    @WhenEvent
    public void triggerPresentar(ComisionServicio comisionServicio, ComisionServicio original, EventContext<Profile,State> eventContext) throws BusinessException {
        //eventContext.updateState(State.);
    }
    @WhenEvent
    public void triggerBack(ComisionServicio comisionServicio, ComisionServicio original, EventContext<Profile,State> eventContext) throws BusinessException {
        //eventContext.updateState(State.);
    }
    @WhenEvent
    public void triggerPresentarDocumentosFirmados(ComisionServicio comisionServicio, ComisionServicio original, EventContext<Profile,State> eventContext) throws BusinessException {
        //eventContext.updateState(State.);
    }
    @WhenEvent
    public void triggerResolver(ComisionServicio comisionServicio, ComisionServicio original, EventContext<Profile,State> eventContext) throws BusinessException {
        //eventContext.updateState(State.);
    }



/***************************************************************************************/
/*************************************** Estados ***************************************/
/***************************************************************************************/

    @OnEnterState
    public void onEnterEntradaDatos(ComisionServicio comisionServicio, EventContext<Profile,State> eventContext) {

    }
    @OnEnterState
    public void onEnterFirmaPorUsuario(ComisionServicio comisionServicio, EventContext<Profile,State> eventContext) {

    }
    @OnEnterState
    public void onEnterResolverPermitirComision(ComisionServicio comisionServicio, EventContext<Profile,State> eventContext) {

    }
    @OnEnterState
    public void onEnterEntregaTickets(ComisionServicio comisionServicio, EventContext<Profile,State> eventContext) {

    }
    @OnEnterState
    public void onEnterAceptado(ComisionServicio comisionServicio, EventContext<Profile,State> eventContext) {

    }




/***************************************************************************************/
/************************** Máquina de Estados del expediente **************************/
/***************************************************************************************/

    //Estados del expediente
    public enum State {
		ENTRADA_DATOS(Profile.CREADOR,true,false,Event.DELETE,Event.PRESENTAR),
		FIRMA_POR_USUARIO(Profile.CREADOR,false,false,Event.BACK,Event.PRESENTAR_DOCUMENTOS_FIRMADOS),
		RESOLVER_PERMITIR_COMISION(Profile.RESPONSABLE,false,false,Event.RESOLVER),
		ENTREGA_TICKETS(null,false,false,Event.RESOLVER),
		ACEPTADO(null,false,true);
                                                        
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
        PRESENTAR,
        BACK,
        PRESENTAR_DOCUMENTOS_FIRMADOS,
        RESOLVER
    }

    public enum Profile {
        CREADOR,
        RESPONSABLE
    }


}