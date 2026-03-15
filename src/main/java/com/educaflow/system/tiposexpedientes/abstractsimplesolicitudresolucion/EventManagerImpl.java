package com.educaflow.system.tiposexpedientes.abstractsimplesolicitudresolucion;

import com.educaflow.subsystem.expedientes.services.EventContext;
import com.educaflow.subsystem.expedientes.services.annotations.OnEnterState;
import com.educaflow.subsystem.expedientes.services.annotations.WhenEvent;
import com.educaflow.subsystem.expedientes.db.AbstractSimpleSolicitudResolucion;
import com.educaflow.subsystem.expedientes.db.repo.AbstractSimpleSolicitudResolucionRepository;
import com.educaflow.base.infrastructure.validation.messages.BusinessException;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class EventManagerImpl extends com.educaflow.subsystem.expedientes.services.EventManager<AbstractSimpleSolicitudResolucion, AbstractSimpleSolicitudResolucion.State, AbstractSimpleSolicitudResolucion.Event,AbstractSimpleSolicitudResolucion.Profile> {

    private final AbstractSimpleSolicitudResolucionRepository repository;
    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Inject
    public EventManagerImpl(AbstractSimpleSolicitudResolucionRepository repository) {
        super(AbstractSimpleSolicitudResolucion.class, AbstractSimpleSolicitudResolucion.State.class, AbstractSimpleSolicitudResolucion.Event.class,AbstractSimpleSolicitudResolucion.Profile.class);
        this.repository = repository;
    }

    @Override
    public void triggerInitialEvent(AbstractSimpleSolicitudResolucion abstractSimpleSolicitudResolucion, EventContext<AbstractSimpleSolicitudResolucion.Profile> eventContext) throws BusinessException {


    }


    @WhenEvent
    public void triggerDelete(AbstractSimpleSolicitudResolucion abstractSimpleSolicitudResolucion, AbstractSimpleSolicitudResolucion original, EventContext<AbstractSimpleSolicitudResolucion.Profile> eventContext) throws BusinessException {
        //abstractSimpleSolicitudResolucion.updateState(AbstractSimpleSolicitudResolucion.State.);
    }
    @WhenEvent
    public void triggerGuardarDatos(AbstractSimpleSolicitudResolucion abstractSimpleSolicitudResolucion, AbstractSimpleSolicitudResolucion original, EventContext<AbstractSimpleSolicitudResolucion.Profile> eventContext) throws BusinessException {
        //abstractSimpleSolicitudResolucion.updateState(AbstractSimpleSolicitudResolucion.State.);
    }
    @WhenEvent
    public void triggerBack(AbstractSimpleSolicitudResolucion abstractSimpleSolicitudResolucion, AbstractSimpleSolicitudResolucion original, EventContext<AbstractSimpleSolicitudResolucion.Profile> eventContext) throws BusinessException {
        //abstractSimpleSolicitudResolucion.updateState(AbstractSimpleSolicitudResolucion.State.);
    }
    @WhenEvent
    public void triggerPresentar(AbstractSimpleSolicitudResolucion abstractSimpleSolicitudResolucion, AbstractSimpleSolicitudResolucion original, EventContext<AbstractSimpleSolicitudResolucion.Profile> eventContext) throws BusinessException {
        //abstractSimpleSolicitudResolucion.updateState(AbstractSimpleSolicitudResolucion.State.);
    }
    @WhenEvent
    public void triggerResolver(AbstractSimpleSolicitudResolucion abstractSimpleSolicitudResolucion, AbstractSimpleSolicitudResolucion original, EventContext<AbstractSimpleSolicitudResolucion.Profile> eventContext) throws BusinessException {
        //abstractSimpleSolicitudResolucion.updateState(AbstractSimpleSolicitudResolucion.State.);
    }



/***************************************************************************************/
/*************************************** Estados ***************************************/
/***************************************************************************************/

    @OnEnterState
    public void onEnterEntradaDatos(AbstractSimpleSolicitudResolucion abstractSimpleSolicitudResolucion, EventContext<AbstractSimpleSolicitudResolucion.Profile> eventContext) {

    }
    @OnEnterState
    public void onEnterPendientePresentacion(AbstractSimpleSolicitudResolucion abstractSimpleSolicitudResolucion, EventContext<AbstractSimpleSolicitudResolucion.Profile> eventContext) {

    }
    @OnEnterState
    public void onEnterPendienteResolucion(AbstractSimpleSolicitudResolucion abstractSimpleSolicitudResolucion, EventContext<AbstractSimpleSolicitudResolucion.Profile> eventContext) {

    }
    @OnEnterState
    public void onEnterAceptado(AbstractSimpleSolicitudResolucion abstractSimpleSolicitudResolucion, EventContext<AbstractSimpleSolicitudResolucion.Profile> eventContext) {

    }
    @OnEnterState
    public void onEnterRechazado(AbstractSimpleSolicitudResolucion abstractSimpleSolicitudResolucion, EventContext<AbstractSimpleSolicitudResolucion.Profile> eventContext) {

    }









}