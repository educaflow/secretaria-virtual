package com.educaflow.system.tiposexpedientes.comision_servicio;

import com.educaflow.shared.expedientes.services.EventContext;
import com.educaflow.shared.expedientes.services.EventManager;
import com.educaflow.shared.expedientes.services.annotations.OnEnterState;
import com.educaflow.shared.expedientes.services.annotations.WhenEvent;
import com.educaflow.shared.expedientes.db.ComisionServicio;
import com.educaflow.shared.expedientes.db.repo.ComisionServicioRepository;
import com.educaflow.base.infrastructure.validation.messages.BusinessException;

import com.google.inject.Inject;



public class EventManagerImpl extends EventManager<ComisionServicio, ComisionServicio.State, ComisionServicio.Event,ComisionServicio.Profile> {

    private final ComisionServicioRepository repository;

    @Inject
    public EventManagerImpl(ComisionServicioRepository repository) {
        super(ComisionServicio.class, ComisionServicio.State.class, ComisionServicio.Event.class,ComisionServicio.Profile.class);
        this.repository = repository;
    }

    @Override
    public void triggerInitialEvent(ComisionServicio comisionServicio, EventContext<ComisionServicio.Profile> eventContext) throws BusinessException {


    }


    @WhenEvent
    public void triggerDelete(ComisionServicio comisionServicio, ComisionServicio original, EventContext<ComisionServicio.Profile> eventContext) throws BusinessException {
        //comisionServicio.updateState(ComisionServicio.State.);
    }
    @WhenEvent
    public void triggerPresentar(ComisionServicio comisionServicio, ComisionServicio original, EventContext<ComisionServicio.Profile> eventContext) throws BusinessException {
        //comisionServicio.updateState(ComisionServicio.State.);
    }
    @WhenEvent
    public void triggerBack(ComisionServicio comisionServicio, ComisionServicio original, EventContext<ComisionServicio.Profile> eventContext) throws BusinessException {
        //comisionServicio.updateState(ComisionServicio.State.);
    }
    @WhenEvent
    public void triggerPresentarDocumentosFirmados(ComisionServicio comisionServicio, ComisionServicio original, EventContext<ComisionServicio.Profile> eventContext) throws BusinessException {
        //comisionServicio.updateState(ComisionServicio.State.);
    }
    @WhenEvent
    public void triggerResolver(ComisionServicio comisionServicio, ComisionServicio original, EventContext<ComisionServicio.Profile> eventContext) throws BusinessException {
        //comisionServicio.updateState(ComisionServicio.State.);
    }



/***************************************************************************************/
/*************************************** Estados ***************************************/
/***************************************************************************************/

    @OnEnterState
    public void onEnterEntradaDatos(ComisionServicio comisionServicio, EventContext<ComisionServicio.Profile> eventContext) {

    }
    @OnEnterState
    public void onEnterFirmaPorUsuario(ComisionServicio comisionServicio, EventContext<ComisionServicio.Profile> eventContext) {

    }
    @OnEnterState
    public void onEnterResolverPermitirComision(ComisionServicio comisionServicio, EventContext<ComisionServicio.Profile> eventContext) {

    }
    @OnEnterState
    public void onEnterEntregaTickets(ComisionServicio comisionServicio, EventContext<ComisionServicio.Profile> eventContext) {

    }
    @OnEnterState
    public void onEnterAceptado(ComisionServicio comisionServicio, EventContext<ComisionServicio.Profile> eventContext) {

    }









}