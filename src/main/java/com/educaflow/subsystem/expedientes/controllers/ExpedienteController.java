package com.educaflow.subsystem.expedientes.controllers;

import com.axelor.auth.db.User;
import org.apache.shiro.authz.UnauthorizedException;
import com.axelor.db.JpaRepository;
import com.axelor.db.modelservice.ModelServiceFactory;
import com.axelor.db.Model;
import com.axelor.i18n.I18n;
import com.axelor.meta.CallMethod;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.educaflow.base.util.SecurityUtil;
import com.educaflow.subsystem.expedientes.services.internal.ExpedienteUtil;
import com.educaflow.subsystem.expedientes.services.internal.ExpedienteLocator;
import com.educaflow.subsystem.expedientes.services.tramitacion.CommonEvent;
import com.educaflow.subsystem.expedientes.services.eventmanager.EventContext;
import com.educaflow.subsystem.expedientes.services.eventmanager.PhaseEventManager;
import com.educaflow.subsystem.expedientes.services.eventmanager.State;
import com.educaflow.subsystem.expedientes.services.tramitacion.Tramitador;
import com.educaflow.subsystem.expedientes.db.Expediente;
import com.educaflow.subsystem.expedientes.db.Profile;
import com.educaflow.subsystem.expedientes.db.TipoExpediente;
import com.educaflow.subsystem.expedientes.db.Tramite;
import com.educaflow.subsystem.expedientes.db.repo.TramiteRepository;
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
    TramiteRepository tramiteRepository;

    @Inject
    Tramitador tramitador;

    @Inject
    ExpedienteLocator expedienteLocator;

    @Inject
    ModelServiceFactory modelServiceFactory;


    public ExpedienteController() {

    }

    @CallMethod
    @Transactional
    public void triggerInitialEvent(ActionRequest actionRequest, ActionResponse response) {
        ActionRequestHelper actionRequestHelper = new ActionRequestHelper(actionRequest);
        ActionResponseHelper actionResponseHelper = new ActionResponseHelper(response);
        try {
            TipoExpediente tipoExpediente = getTipoExpedienteFromIdTramite(actionRequestHelper.getId());
            String profileName = actionRequestHelper.getProfileName();
            EventContext eventContext = getEventContext(null,tipoExpediente,profileName);

            Expediente expediente = tramitador.triggerInitialEvent(tipoExpediente, eventContext);

            PhaseEventManager phaseEventManager = expedienteLocator.getPhaseEventManager(tipoExpediente, expediente.getCodePhase());
            String viewName = phaseEventManager.getViewName(expediente, eventContext);
            actionResponseHelper.doResponseViewForm(viewName, phaseEventManager.getModelClass(), expediente, getTabName(expediente), eventContext.getProfile().name());

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
            Expediente expediente = ExpedienteUtil.getExpedienteFromIdExpediente(actionRequestHelper.getId());
            String eventName = actionRequestHelper.getEventName();
            Map<String, Object> requestData = actionRequestHelper.getRequestData();
            PhaseEventManager phaseEventManager = expedienteLocator.getPhaseEventManager(expediente.getTipoExpediente(), expediente.getCodePhase());
            String profileName = actionRequestHelper.getProfileName();
            EventContext eventContext = getEventContext(expediente,expediente.getTipoExpediente(),profileName);

            if (eventName.equals(CommonEvent.EXIT.name())) {
                response.setSignal("refresh-app", null);
                return;
            }

            tramitador.triggerEvent(expediente, eventName, requestData, eventContext);

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
            Expediente expediente = ExpedienteUtil.getExpedienteFromIdExpediente(actionRequestHelper.getId());
            PhaseEventManager phaseEventManager = expedienteLocator.getPhaseEventManager(expediente.getTipoExpediente(), expediente.getCodePhase());
            String profileName = actionRequestHelper.getProfileName();
            EventContext eventContext = getEventContext(expediente,expediente.getTipoExpediente(),profileName);

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
            Expediente expediente = ExpedienteUtil.getExpedienteFromIdExpediente(actionRequestHelper.getParentId());
            Class<? extends Model> beanClass = actionRequestHelper.getModelClass();
            Map<String, Object> requestData = actionRequestHelper.getRequestData();

            Model bean=findModel(beanClass, actionRequestHelper.getId());
            String validateProperty=actionRequestHelper.getParentSource();

            BusinessMessages businessMessages = tramitador.validateChild(expediente, bean,beanClass, validateProperty,requestData);

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

    private TipoExpediente getTipoExpedienteFromIdTramite(long idTramite) {
        Tramite tramite = tramiteRepository.find(idTramite);
        if (tramite == null) {
            throw new RuntimeException("No existe el tramite con idTramite: " + idTramite);
        }
        TipoExpediente tipoExpediente = tramite.getDefaultTipoExpediente();
        if (tipoExpediente == null) {
            throw new RuntimeException("No existe el tipo de expediente para el tramite con idTramite: " + idTramite);
        }
        return tipoExpediente;
    }




    private Model findModel(Class<? extends Model> classModel, Long id) {
        try {
            Model model;
            if (id==null) {
                model=classModel.getConstructor().newInstance();
            } else {
                JpaRepository<? extends Model> repository = JpaRepository.of(classModel);
                model=repository.find(Convert.objectToLong(id));
            }

            return model;
        } catch (Exception ex) {
            throw new RuntimeException("Error al encontrar el modelo: " + classModel.getName() + " con id: " + id, ex);
        }
    }


    /*******************************************************************/
    /********************** Funciones de Utilidad **********************/
    /*******************************************************************/

    /**
     * El contexto del evento. El perfil viene de la petición del cliente, así que además de parsearlo
     * contra el enum global hay que comprobar que es uno de los perfiles que USA este tipo de
     * expediente: el enum global acepta perfiles que el tipo no tiene, y getViewName tiene fallback a
     * la vista sin perfil, de modo que sin esta comprobación una petición con un perfil ajeno podría
     * llegar a renderizar una vista en vez de fallar. Es la detección que hasta ahora daba el
     * Enum.valueOf sobre el enum Profile por tipo.
     */
    private EventContext getEventContext(Expediente expediente, TipoExpediente tipoExpediente, String profileName) {
        Profile profile;
        try {
            profile = Profile.valueOf(profileName);
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException("El perfil '" + profileName + "' no existe.", ex);
        }

        checkProfileDelTipoExpediente(profile, tipoExpediente);

        Centro centro = getCentroFromCurrentUser();

        return new EventContext(expediente, profile, centro, modelServiceFactory);
    }

    private static void checkProfileDelTipoExpediente(Profile profile, TipoExpediente tipoExpediente) {
        for (State state : tipoExpediente.getTipoExpedienteStates().getStates()) {
            if (profile.equals(state.getProfile())) {
                return;
            }
        }

        throw new RuntimeException("El perfil '" + profile.name() + "' no lo usa ningún estado del tipo de expediente " + tipoExpediente.getCode() + ".");
    }


    private static Centro getCentroFromCurrentUser() {
        final User user = SecurityUtil.getUser();

        if (user == null) {
            throw new RuntimeException("User es null");
        }

        Centro centro = user.getCentroActivo();

        if (centro == null) {
            throw new RuntimeException("El centro activo es null para el usuario: " + user.getName());
        }

        return centro;
    }





}