package com.educaflow.system.tiposexpedientes.prueba;

import com.educaflow.shared.expedientes.services.EventContext;
import com.educaflow.shared.expedientes.services.EventManager;
import com.educaflow.shared.expedientes.services.annotations.OnEnterState;
import com.educaflow.shared.expedientes.services.annotations.WhenEvent;
import com.educaflow.shared.expedientes.db.Prueba;
import com.educaflow.shared.expedientes.db.repo.PruebaRepository;
import com.educaflow.base.infrastructure.validation.messages.BusinessException;
import com.google.inject.Inject;


public class EventManagerImpl extends EventManager<Prueba,Prueba.State,Prueba.Event,Prueba.Profile> {

    private final PruebaRepository pruebaRepository;

    @Inject
    public EventManagerImpl(PruebaRepository pruebaRepository) {
        super(Prueba.class, Prueba.State.class, Prueba.Event.class,Prueba.Profile.class);
        this.pruebaRepository = pruebaRepository;
    }

    @Override
    public void triggerInitialEvent(Prueba prueba, EventContext<Prueba.Profile> eventContext) throws BusinessException {

    }


    @WhenEvent
    public void triggerPresentar(Prueba prueba,Prueba pruebaOriginal, EventContext<Prueba.Profile> eventContext) throws BusinessException {
        prueba.updateState(Prueba.State.REVISION);
    }


    @WhenEvent
    public void triggerSubsanar(Prueba prueba,Prueba pruebaOriginal, EventContext<Prueba.Profile> eventContext) throws BusinessException {
        prueba.updateState(Prueba.State.ENTRADA_DATOS);
    }

    @WhenEvent
    public void triggerAceptar(Prueba prueba,Prueba pruebaOriginal, EventContext<Prueba.Profile> eventContext) throws BusinessException {
        prueba.updateState(Prueba.State.ACEPTADO);
    }

    @WhenEvent
    public void triggerRechazar(Prueba prueba,Prueba pruebaOriginal, EventContext<Prueba.Profile> eventContext) throws BusinessException {
        prueba.updateState(Prueba.State.RECHAZADO);
    }

    @WhenEvent
    public void triggerDelete(Prueba prueba, Prueba original, EventContext<Prueba.Profile> eventContext) throws BusinessException {

    }


    @OnEnterState
    public void onEnterEntradaDatos(Prueba prueba, EventContext<Prueba.Profile> eventContext) {

    }

    @OnEnterState
    public void onEnterRevision(Prueba prueba, EventContext<Prueba.Profile> eventContext) {

    }

    @OnEnterState
    public void onEnterAceptado(Prueba prueba, EventContext<Prueba.Profile> eventContext) {

    }

    @OnEnterState
    public void onEnterRechazado(Prueba prueba, EventContext<Prueba.Profile> eventContext) {

    }


}
