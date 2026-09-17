package com.educaflow.subsystem.expedientes.controllers;

import org.apache.shiro.authz.UnauthorizedException;
import com.axelor.db.JpaRepository;
import com.axelor.db.modelservice.ModelServiceFactory;
import com.axelor.db.Model;
import com.axelor.i18n.I18n;
import com.axelor.meta.CallMethod;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.educaflow.subsystem.expedientes.services.ExpedienteService;
import com.educaflow.subsystem.expedientes.services.internal.ExpedienteLocator;
import com.educaflow.subsystem.expedientes.services.tramitacion.CommonEvent;
import com.educaflow.subsystem.expedientes.services.eventmanager.EventContext;
import com.educaflow.subsystem.expedientes.services.eventmanager.InitialEventContext;
import com.educaflow.subsystem.expedientes.services.eventmanager.PhaseEventManager;
import com.educaflow.subsystem.expedientes.services.eventmanager.State;
import com.educaflow.subsystem.expedientes.db.Expediente;
import com.educaflow.subsystem.expedientes.db.ContextoTramitacion;
import com.educaflow.subsystem.expedientes.db.Profile;
import com.educaflow.subsystem.expedientes.db.TipoExpediente;
import com.educaflow.subsystem.expedientes.db.Tramite;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.base.infrastructure.axelorhelper.ActionRequestHelper;
import com.educaflow.base.infrastructure.axelorhelper.ActionResponseHelper;
import com.educaflow.base.util.Convert;
import com.educaflow.base.infrastructure.validation.messages.BusinessException;
import com.axelor.db.modelservice.BusinessMessages;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;


import java.util.*;



public class ExpedienteController {

    @Inject
    ExpedienteService expedienteService;

    @Inject
    ExpedienteLocator expedienteLocator;

    @Inject
    ModelServiceFactory modelServiceFactory;


    public ExpedienteController() {

    }

    @CallMethod
    //La transacción abarca también la resolución de la vista: si esta falla, el alta se deshace.
    @Transactional
    public void triggerInitialEvent(ActionRequest actionRequest, ActionResponse response) {
        ActionRequestHelper actionRequestHelper = new ActionRequestHelper(actionRequest);
        ActionResponseHelper actionResponseHelper = new ActionResponseHelper(response);
        try {
            ContextoTramitacion contextoTramitacion = actionRequest.getContext().asType(ContextoTramitacion.class);

            Expediente expediente = expedienteService.crear(contextoTramitacion);

            EventContext eventContext = new EventContext(expediente, contextoTramitacion.getProfile(), modelServiceFactory);
            PhaseEventManager phaseEventManager = expedienteLocator.getPhaseEventManager(expediente.getTipoExpediente(), expediente.getCodePhase());
            String viewName = phaseEventManager.getViewName(expediente, eventContext);
            actionResponseHelper.doResponseViewForm(viewName, phaseEventManager.getModelClass(), expediente, getTabName(expediente), eventContext.getProfile().name());
            response.setCanClose(true);

        } catch (BusinessException ex) {
            actionResponseHelper.doResponseBusinessMessagesAsError("No es posible crear el expediente", ex.getBusinessMessages());
            return;
        } catch (UnauthorizedException ex) {
            // MUST NOT envolverla: sin envolver, ResponseInterceptor la reconoce como
            // AuthorizationException y responde un error de acceso (403) en vez de un 500 genérico.
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }


    @CallMethod
    @Transactional
    public void triggerEvent(ActionRequest request, ActionResponse response) {
        ActionRequestHelper actionRequestHelper = new ActionRequestHelper(request);
        ActionResponseHelper actionResponseHelper = new ActionResponseHelper(response);
        try {
            Expediente expediente = expedienteService.getExpediente(actionRequestHelper.getId());
            String eventName = actionRequestHelper.getEventName();
            Map<String, Object> requestData = actionRequestHelper.getRequestData();
            PhaseEventManager phaseEventManager = expedienteLocator.getPhaseEventManager(expediente.getTipoExpediente(), expediente.getCodePhase());
            String profileName = actionRequestHelper.getProfileName();
            EventContext eventContext = new EventContext(expediente, Profile.valueOf(profileName), modelServiceFactory);

            if (eventName.equals(CommonEvent.EXIT.name())) {
                response.setSignal("refresh-app", null);
                return;
            }

            expedienteService.triggerEvent(expediente, eventName, requestData, eventContext);

            if (eventName.equals(CommonEvent.DELETE.name())) {
                response.setSignal("refresh-app", null);
            } else {
                String viewName = phaseEventManager.getViewName(expediente, eventContext);
                actionResponseHelper.doResponseViewForm(viewName, phaseEventManager.getModelClass(), expediente, getTabName(expediente), eventContext.getProfile().name());
            }

        } catch (BusinessException ex) {
            actionResponseHelper.doResponseBusinessMessages(ex.getBusinessMessages());
        } catch (UnauthorizedException ex) {
            // MUST NOT envolverla: sin envolver, ResponseInterceptor la reconoce como
            // AuthorizationException y responde un error de acceso (403) en vez de un 500 genérico.
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @CallMethod
    public void viewExpediente(ActionRequest request, ActionResponse response) {
        ActionRequestHelper actionRequestHelper = new ActionRequestHelper(request);
        ActionResponseHelper actionResponseHelper = new ActionResponseHelper(response);
        try {
            Expediente expediente = expedienteService.getExpediente(actionRequestHelper.getId());
            PhaseEventManager phaseEventManager = expedienteLocator.getPhaseEventManager(expediente.getTipoExpediente(), expediente.getCodePhase());
            String profileName = actionRequestHelper.getProfileName();
            EventContext eventContext = new EventContext(expediente, Profile.valueOf(profileName), modelServiceFactory);

            String viewName = phaseEventManager.getViewName(expediente, eventContext);
            actionResponseHelper.doResponseViewForm(viewName, phaseEventManager.getModelClass(), expediente, getTabName(expediente), eventContext.getProfile().name());

        } catch (UnauthorizedException ex) {
            // MUST NOT envolverla: sin envolver, ResponseInterceptor la reconoce como
            // AuthorizationException y responde un error de acceso (403) en vez de un 500 genérico.
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @CallMethod
    public void validateChild(ActionRequest request, ActionResponse response) {
        ActionRequestHelper actionRequestHelper = new ActionRequestHelper(request);
        ActionResponseHelper actionResponseHelper = new ActionResponseHelper(response);
        try {
            Expediente expediente = expedienteService.getExpediente(actionRequestHelper.getParentId());
            Class<? extends Model> beanClass = actionRequestHelper.getModelClass();
            Map<String, Object> requestData = actionRequestHelper.getRequestData();

            Model bean = findModel(beanClass, actionRequestHelper.getId());
            String validateProperty = actionRequestHelper.getParentSource();

            BusinessMessages businessMessages = expedienteService.validateChild(expediente, bean, beanClass, validateProperty, requestData);

            actionResponseHelper.doResponseBusinessMessages(businessMessages);

        } catch (UnauthorizedException ex) {
            // MUST NOT envolverla: sin envolver, ResponseInterceptor la reconoce como
            // AuthorizationException y responde un error de acceso (403) en vez de un 500 genérico.
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }


    /*******************************************************************/
    /********************** Funciones de Negocio  **********************/
    /*******************************************************************/


    private String getTabName(Expediente expediente) {
        return expediente.getNumeroExpediente() + "-" + I18n.get(expediente.getTipoExpediente().getName());
    }


    /*******************************************************************/
    /*************** Funciones de Acceso a Base de datos ***************/
    /*******************************************************************/


    private Model findModel(Class<? extends Model> classModel, Long id) {
        try {
            Model model;
            if (id == null) {
                model = classModel.getConstructor().newInstance();
            } else {
                JpaRepository<? extends Model> repository = JpaRepository.of(classModel);
                model = repository.find(Convert.objectToLong(id));
            }

            return model;
        } catch (Exception ex) {
            throw new RuntimeException("Error al encontrar el modelo: " + classModel.getName() + " con id: " + id, ex);
        }
    }

}
