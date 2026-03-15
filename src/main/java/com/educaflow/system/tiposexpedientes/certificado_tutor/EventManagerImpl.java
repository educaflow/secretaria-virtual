package com.educaflow.system.tiposexpedientes.certificado_tutor;

import com.educaflow.shared.expedientes.services.EventContext;
import com.educaflow.shared.expedientes.services.EventManager;
import com.educaflow.shared.expedientes.services.annotations.OnEnterState;
import com.educaflow.shared.expedientes.services.annotations.WhenEvent;
import com.educaflow.shared.expedientes.db.CertificadoTutor;
import com.educaflow.shared.expedientes.db.repo.CertificadoTutorRepository;
import com.educaflow.base.infrastructure.validation.messages.BusinessException;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class EventManagerImpl extends EventManager<CertificadoTutor, CertificadoTutor.State, CertificadoTutor.Event,CertificadoTutor.Profile> {

    private final CertificadoTutorRepository repository;
    protected final Logger log = LoggerFactory.getLogger(getClass());


    @Inject
    public EventManagerImpl(CertificadoTutorRepository repository) {
        super(CertificadoTutor.class, CertificadoTutor.State.class, CertificadoTutor.Event.class,CertificadoTutor.Profile.class);
        this.repository = repository;
    }

    @Override
    public void triggerInitialEvent(CertificadoTutor certificadoTutor, EventContext<CertificadoTutor.Profile> eventContext) throws BusinessException {


    }


    @WhenEvent
    public void triggerDelete(CertificadoTutor certificadoTutor, CertificadoTutor original, EventContext<CertificadoTutor.Profile> eventContext) throws BusinessException {
        //certificadoTutor.updateState(CertificadoTutor.State.);
    }
    @WhenEvent
    public void triggerPresentar(CertificadoTutor certificadoTutor, CertificadoTutor original, EventContext<CertificadoTutor.Profile> eventContext) throws BusinessException {
        //certificadoTutor.updateState(CertificadoTutor.State.);
    }
    @WhenEvent
    public void triggerSubsanar(CertificadoTutor certificadoTutor, CertificadoTutor original, EventContext<CertificadoTutor.Profile> eventContext) throws BusinessException {
        //certificadoTutor.updateState(CertificadoTutor.State.);
    }
    @WhenEvent
    public void triggerAceptar(CertificadoTutor certificadoTutor, CertificadoTutor original, EventContext<CertificadoTutor.Profile> eventContext) throws BusinessException {
        //certificadoTutor.updateState(CertificadoTutor.State.);
    }
    @WhenEvent
    public void triggerRechazar(CertificadoTutor certificadoTutor, CertificadoTutor original, EventContext<CertificadoTutor.Profile> eventContext) throws BusinessException {
        //certificadoTutor.updateState(CertificadoTutor.State.);
    }



/***************************************************************************************/
/*************************************** Estados ***************************************/
/***************************************************************************************/

    @OnEnterState
    public void onEnterEntradaDatos(CertificadoTutor certificadoTutor, EventContext<CertificadoTutor.Profile> eventContext) {

    }
    @OnEnterState
    public void onEnterRevision(CertificadoTutor certificadoTutor, EventContext<CertificadoTutor.Profile> eventContext) {

    }
    @OnEnterState
    public void onEnterAceptado(CertificadoTutor certificadoTutor, EventContext<CertificadoTutor.Profile> eventContext) {

    }
    @OnEnterState
    public void onEnterRechazado(CertificadoTutor certificadoTutor, EventContext<CertificadoTutor.Profile> eventContext) {

    }









}