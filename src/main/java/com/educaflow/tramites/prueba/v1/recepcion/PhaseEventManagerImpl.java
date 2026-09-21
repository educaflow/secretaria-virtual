package com.educaflow.tramites.prueba.v1.recepcion;

import com.educaflow.subsystem.tramitador.tramitacion.eventmanager.EventContext;
import com.educaflow.subsystem.tramitador.tramitacion.eventmanager.OnEnterState;
import com.educaflow.subsystem.tramitador.tramitacion.eventmanager.PhaseEventManager;
import com.educaflow.subsystem.tramitador.tramitacion.eventmanager.WhenEvent;
import com.educaflow.subsystem.expedientes.db.PruebaV1;
import com.educaflow.subsystem.expedientes.db.repo.PruebaV1Repository;
import com.educaflow.base.infrastructure.validation.messages.BusinessException;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class PhaseEventManagerImpl extends PhaseEventManager<PruebaV1> {

    private final PruebaV1Repository repository;
    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Inject
    public PhaseEventManagerImpl(PruebaV1Repository repository) {
        super(PruebaV1.class);
        this.repository = repository;
    }



    @WhenEvent
    public void triggerDelete(PruebaV1 prueba, PruebaV1 original, EventContext eventContext) throws BusinessException {
        //eventContext.updateState(States.Recepcion.);
    }
    @WhenEvent
    public void triggerPresentar(PruebaV1 prueba, PruebaV1 original, EventContext eventContext) throws BusinessException {
        //eventContext.updateState(States.Recepcion.);
    }



/***************************************************************************************/
/*************************************** Estados ***************************************/
/***************************************************************************************/

    @OnEnterState
    public void onEnterEntradaDatos(PruebaV1 prueba, EventContext eventContext) {

    }

}
