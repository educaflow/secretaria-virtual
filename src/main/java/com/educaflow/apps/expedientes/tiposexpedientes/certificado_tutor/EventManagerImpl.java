package com.educaflow.apps.expedientes.tiposexpedientes.certificado_tutor;

import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.User;
import com.axelor.db.JpaRepository;
import com.educaflow.apps.expedientes.common.EventContext;
import com.educaflow.apps.expedientes.common.annotations.OnEnterState;
import com.educaflow.apps.expedientes.common.annotations.WhenEvent;
import com.educaflow.apps.expedientes.db.CertificadoTutor;
import com.educaflow.apps.expedientes.db.repo.CertificadoTutorRepository;
import com.educaflow.common.validation.messages.BusinessException;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class EventManagerImpl extends com.educaflow.apps.expedientes.common.EventManager<CertificadoTutor, CertificadoTutor.State, CertificadoTutor.Event,CertificadoTutor.Profile> {

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