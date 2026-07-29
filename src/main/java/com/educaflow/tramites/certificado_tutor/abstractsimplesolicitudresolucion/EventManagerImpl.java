package com.educaflow.tramites.certificado_tutor.abstractsimplesolicitudresolucion;

import com.axelor.inject.Beans;
import com.educaflow.subsystem.expedientes.services.eventmanager.EventContext;
import com.educaflow.subsystem.expedientes.services.eventmanager.OnEnterState;
import com.educaflow.subsystem.expedientes.services.eventmanager.WhenEvent;
import com.educaflow.subsystem.expedientes.db.AbstractSimpleSolicitudResolucion;
import com.educaflow.subsystem.expedientes.db.repo.AbstractSimpleSolicitudResolucionRepository;
import com.educaflow.base.infrastructure.validation.messages.BusinessException;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class EventManagerImpl extends com.educaflow.subsystem.expedientes.services.eventmanager.EventManager<AbstractSimpleSolicitudResolucion, EventManagerImpl.State, EventManagerImpl.Event,EventManagerImpl.Profile> {

    private final AbstractSimpleSolicitudResolucionRepository repository;
    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Inject
    public EventManagerImpl(AbstractSimpleSolicitudResolucionRepository repository) {
        super(AbstractSimpleSolicitudResolucion.class, State.class, Event.class,Profile.class);
        this.repository = repository;
    }

    @Override
    public void triggerInitialEvent(AbstractSimpleSolicitudResolucion abstractSimpleSolicitudResolucion, EventContext<Profile,State> eventContext) throws BusinessException {


    }


    @WhenEvent
    public void triggerDelete(AbstractSimpleSolicitudResolucion abstractSimpleSolicitudResolucion, AbstractSimpleSolicitudResolucion original, EventContext<Profile,State> eventContext) throws BusinessException {
        //eventContext.updateState(State.);
    }
    @WhenEvent
    public void triggerGuardarDatos(AbstractSimpleSolicitudResolucion abstractSimpleSolicitudResolucion, AbstractSimpleSolicitudResolucion original, EventContext<Profile,State> eventContext) throws BusinessException {
        //eventContext.updateState(State.);
    }
    @WhenEvent
    public void triggerBack(AbstractSimpleSolicitudResolucion abstractSimpleSolicitudResolucion, AbstractSimpleSolicitudResolucion original, EventContext<Profile,State> eventContext) throws BusinessException {
        //eventContext.updateState(State.);
    }
    @WhenEvent
    public void triggerPresentar(AbstractSimpleSolicitudResolucion abstractSimpleSolicitudResolucion, AbstractSimpleSolicitudResolucion original, EventContext<Profile,State> eventContext) throws BusinessException {
        //eventContext.updateState(State.);
    }
    @WhenEvent
    public void triggerResolver(AbstractSimpleSolicitudResolucion abstractSimpleSolicitudResolucion, AbstractSimpleSolicitudResolucion original, EventContext<Profile,State> eventContext) throws BusinessException {
        //eventContext.updateState(State.);
    }



/***************************************************************************************/
/*************************************** Estados ***************************************/
/***************************************************************************************/

    @OnEnterState
    public void onEnterEntradaDatos(AbstractSimpleSolicitudResolucion abstractSimpleSolicitudResolucion, EventContext<Profile,State> eventContext) {

    }
    @OnEnterState
    public void onEnterPendientePresentacion(AbstractSimpleSolicitudResolucion abstractSimpleSolicitudResolucion, EventContext<Profile,State> eventContext) {

    }
    @OnEnterState
    public void onEnterPendienteResolucion(AbstractSimpleSolicitudResolucion abstractSimpleSolicitudResolucion, EventContext<Profile,State> eventContext) {

    }
    @OnEnterState
    public void onEnterAceptado(AbstractSimpleSolicitudResolucion abstractSimpleSolicitudResolucion, EventContext<Profile,State> eventContext) {

    }
    @OnEnterState
    public void onEnterRechazado(AbstractSimpleSolicitudResolucion abstractSimpleSolicitudResolucion, EventContext<Profile,State> eventContext) {

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