package com.educaflow.tramites.certificado_tutor.v1;

import com.axelor.inject.Beans;
import com.educaflow.subsystem.expedientes.services.eventmanager.EventContext;
import com.educaflow.subsystem.expedientes.services.eventmanager.OnEnterState;
import com.educaflow.subsystem.expedientes.services.eventmanager.WhenEvent;
import com.educaflow.subsystem.expedientes.db.CertificadoTutorV1;
import com.educaflow.subsystem.expedientes.db.repo.CertificadoTutorV1Repository;
import com.educaflow.base.infrastructure.validation.messages.BusinessException;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class EventManagerImpl extends com.educaflow.subsystem.expedientes.services.eventmanager.EventManager<CertificadoTutorV1, EventManagerImpl.State, EventManagerImpl.Event,EventManagerImpl.Profile> {

    private final CertificadoTutorV1Repository repository;
    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Inject
    public EventManagerImpl(CertificadoTutorV1Repository repository) {
        super(CertificadoTutorV1.class, State.class, Event.class,Profile.class);
        this.repository = repository;
    }

    @Override
    public void triggerInitialEvent(CertificadoTutorV1 certificadoTutor, EventContext<Profile,State> eventContext) throws BusinessException {


    }


    @WhenEvent
    public void triggerDelete(CertificadoTutorV1 certificadoTutor, CertificadoTutorV1 original, EventContext<Profile,State> eventContext) throws BusinessException {
        //eventContext.updateState(State.);
    }
    @WhenEvent
    public void triggerPresentar(CertificadoTutorV1 certificadoTutor, CertificadoTutorV1 original, EventContext<Profile,State> eventContext) throws BusinessException {
        //eventContext.updateState(State.);
    }
    @WhenEvent
    public void triggerSubsanar(CertificadoTutorV1 certificadoTutor, CertificadoTutorV1 original, EventContext<Profile,State> eventContext) throws BusinessException {
        //eventContext.updateState(State.);
    }
    @WhenEvent
    public void triggerAceptar(CertificadoTutorV1 certificadoTutor, CertificadoTutorV1 original, EventContext<Profile,State> eventContext) throws BusinessException {
        //eventContext.updateState(State.);
    }
    @WhenEvent
    public void triggerRechazar(CertificadoTutorV1 certificadoTutor, CertificadoTutorV1 original, EventContext<Profile,State> eventContext) throws BusinessException {
        //eventContext.updateState(State.);
    }



/***************************************************************************************/
/*************************************** Estados ***************************************/
/***************************************************************************************/

    @OnEnterState
    public void onEnterEntradaDatos(CertificadoTutorV1 certificadoTutor, EventContext<Profile,State> eventContext) {

    }
    @OnEnterState
    public void onEnterRevision(CertificadoTutorV1 certificadoTutor, EventContext<Profile,State> eventContext) {

    }
    @OnEnterState
    public void onEnterAceptado(CertificadoTutorV1 certificadoTutor, EventContext<Profile,State> eventContext) {

    }
    @OnEnterState
    public void onEnterRechazado(CertificadoTutorV1 certificadoTutor, EventContext<Profile,State> eventContext) {

    }




/***************************************************************************************/
/************************** Máquina de Estados del expediente **************************/
/***************************************************************************************/

    //Estados del expediente
    public enum State {
		ENTRADA_DATOS(Profile.CREADOR,true,false,Event.DELETE,Event.PRESENTAR),
		REVISION(Profile.RESPONSABLE,false,false,Event.SUBSANAR,Event.ACEPTAR,Event.RECHAZAR),
		ACEPTADO(Profile.RESPONSABLE,false,true,Event.SUBSANAR),
		RECHAZADO(Profile.RESPONSABLE,false,true,Event.SUBSANAR);
                                                        
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
        SUBSANAR,
        ACEPTAR,
        RECHAZAR
    }

    public enum Profile {
        CREADOR,
        RESPONSABLE
    }


}