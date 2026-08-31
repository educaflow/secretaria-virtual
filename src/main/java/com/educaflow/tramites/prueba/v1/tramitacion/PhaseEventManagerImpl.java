package com.educaflow.tramites.prueba.v1.tramitacion;

import com.axelor.inject.Beans;
import com.educaflow.subsystem.expedientes.services.eventmanager.EventContext;
import com.educaflow.subsystem.expedientes.services.eventmanager.OnEnterState;
import com.educaflow.subsystem.expedientes.services.eventmanager.WhenEvent;
import com.educaflow.subsystem.expedientes.db.PruebaV1;
import com.educaflow.subsystem.expedientes.db.repo.PruebaV1Repository;
import com.educaflow.base.infrastructure.validation.messages.BusinessException;
import com.educaflow.tramites.prueba.v1.States;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class PhaseEventManagerImpl extends com.educaflow.subsystem.expedientes.services.eventmanager.PhaseEventManager<PruebaV1> {

    private final PruebaV1Repository repository;
    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Inject
    public PhaseEventManagerImpl(PruebaV1Repository repository) {
        super(PruebaV1.class);
        this.repository = repository;
    }


    @WhenEvent
    public void triggerSubsanar(PruebaV1 prueba, PruebaV1 original, EventContext eventContext) throws BusinessException {
        //eventContext.updateState(States.Tramitacion.);
    }
    @WhenEvent
    public void triggerAceptar(PruebaV1 prueba, PruebaV1 original, EventContext eventContext) throws BusinessException {
        //eventContext.updateState(States.Tramitacion.);
    }
    @WhenEvent
    public void triggerRechazar(PruebaV1 prueba, PruebaV1 original, EventContext eventContext) throws BusinessException {
        //eventContext.updateState(States.Tramitacion.);
    }



/***************************************************************************************/
/*************************************** Estados ***************************************/
/***************************************************************************************/

    @OnEnterState
    public void onEnterRevision(PruebaV1 prueba, EventContext eventContext) {

    }
    @OnEnterState
    public void onEnterAceptado(PruebaV1 prueba, EventContext eventContext) {

    }
    @OnEnterState
    public void onEnterRechazado(PruebaV1 prueba, EventContext eventContext) {

    }

}
