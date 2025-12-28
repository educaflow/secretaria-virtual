package com.educaflow.apps.expedientes.common;

import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.User;
import com.axelor.db.JPA;
import com.axelor.db.JpaRepository;
import com.axelor.db.Model;
import com.axelor.i18n.I18n;
import com.axelor.inject.Beans;
import com.axelor.meta.CallMethod;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.educaflow.apps.expedientes.common.tramitador.Tramitador;
import com.educaflow.apps.expedientes.db.Expediente;
import com.educaflow.apps.expedientes.db.TipoExpediente;
import com.educaflow.apps.expedientes.db.Tramite;
import com.educaflow.apps.expedientes.db.repo.TramiteRepository;
import com.educaflow.apps.configuracioncentro.db.Centro;
import com.educaflow.common.util.ActionRequestHelper;
import com.educaflow.common.util.AxelorViewUtil;
import com.educaflow.common.util.Convert;
import com.educaflow.common.validation.messages.BusinessException;
import com.educaflow.common.validation.messages.BusinessMessages;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;


import java.util.*;



public class ExpedienteController {

    @Inject
    TramiteRepository tramiteRepository;

    @Inject
    Tramitador tramitador;


    public ExpedienteController() {

    }

    @CallMethod
    @Transactional
    public void triggerInitialEvent(ActionRequest actionRequest, ActionResponse response) {
        try {
            ActionRequestHelper actionRequestHelper = new ActionRequestHelper(actionRequest);

            TipoExpediente tipoExpediente = getTipoExpedienteFromIdTramite(actionRequestHelper.getId());
            EventManager eventManager = tipoExpediente.getEventManager();
            String profileName = actionRequestHelper.getProfileName();
            EventContext eventContext = getEventContext(eventManager,profileName);

            Expediente expediente = tramitador.triggerInitialEvent(tipoExpediente, eventContext);

            String viewName = eventManager.getViewName(expediente, eventContext);
            AxelorViewUtil.doResponseViewForm(response, viewName, eventManager.getModelClass(), expediente, getTabName(expediente), eventContext.getProfile().name());

        } catch (BusinessException ex) {
            AxelorViewUtil.doResponseBusinessMessagesAsError(response, "No es posible crear el expediente", ex.getBusinessMessages());
            return;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }


    @CallMethod
    @Transactional
    public void triggerEvent(ActionRequest request, ActionResponse response) {
        try {
            ActionRequestHelper actionRequestHelper = new ActionRequestHelper(request);

            Expediente expediente = getExpedienteFromIdExpediente(actionRequestHelper.getId());
            String eventName = actionRequestHelper.getEventName();
            Map<String, Object> requestData = actionRequestHelper.getRequestData();
            EventManager eventManager = expediente.getTipoExpediente().getEventManager();
            String profileName = actionRequestHelper.getProfileName();
            EventContext eventContext = getEventContext(eventManager,profileName);



            if (eventName.equals(CommonEvent.EXIT.name())) {
                response.setSignal("refresh-app", null);
                return;
            }

            tramitador.triggerEvent(expediente, eventName, requestData, eventContext);


            if (eventName.equals(CommonEvent.DELETE.name())) {
                response.setSignal("refresh-app", null);
            } else {
                String viewName = eventManager.getViewName(expediente, eventContext);
                AxelorViewUtil.doResponseViewForm(response, viewName, eventManager.getModelClass(), expediente, getTabName(expediente), eventContext.getProfile().name());
            }

        } catch (BusinessException ex) {
            AxelorViewUtil.doResponseBusinessMessages(response, ex.getBusinessMessages());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @CallMethod
    public void viewExpediente(ActionRequest request, ActionResponse response) {
        try {
            ActionRequestHelper actionRequestHelper = new ActionRequestHelper(request);

            Expediente expediente = getExpedienteFromIdExpediente(actionRequestHelper.getId());
            EventManager eventManager = expediente.getTipoExpediente().getEventManager();
            String profileName = actionRequestHelper.getProfileName();
            EventContext eventContext = getEventContext(eventManager,profileName);

            String viewName = eventManager.getViewName(expediente, eventContext);
            AxelorViewUtil.doResponseViewForm(response, viewName, eventManager.getModelClass(), expediente, getTabName(expediente), eventContext.getProfile().name());

        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @CallMethod
    public void validateChild(ActionRequest request, ActionResponse response) {
        try {
            ActionRequestHelper actionRequestHelper = new ActionRequestHelper(request);

            Expediente expediente = getExpedienteFromIdExpediente(actionRequestHelper.getParentId());
            Class<? extends Model> beanClass = actionRequestHelper.getModelClass();
            Map<String, Object> requestData = actionRequestHelper.getRequestData();

            Model bean=findModel(beanClass, actionRequestHelper.getId());
            String validateProperty=actionRequestHelper.getParentSource();

            BusinessMessages businessMessages = tramitador.validateChild(expediente, bean,beanClass, validateProperty,requestData);

            AxelorViewUtil.doResponseBusinessMessages(response, businessMessages);

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


    private Expediente getExpedienteFromIdExpediente(long idExpediente) {
        JpaRepository<Expediente> expedienteRepository = getJpaRepository(idExpediente);
        Expediente expediente =expedienteRepository.find(idExpediente);
        if (expediente == null) {
            throw new RuntimeException("No existe el expediente con idExpediente: " + idExpediente);
        }

        return expediente;
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

    public <T extends Enum<T>> EventContext<T> getEventContext(EventManager eventManager, String profileName) {
        try {
            Enum profile = Enum.valueOf(eventManager.getProfileClass(), profileName);
            Centro centro = getCentroFromCurrentUser();
            return new EventContext<>(profile, centro);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    private static Centro getCentroFromCurrentUser() {
        final User user = AuthUtils.getUser();

        if (user == null) {
            throw new RuntimeException("User es null");
        }

        Centro centro = user.getCentroActivo();

        if (centro == null) {
            String codigoCentroDefecto = "460001";
            System.out.println("ERROR:El usuario no tiene un centro activo, se asigna el centro por defecto '" + codigoCentroDefecto + "' !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            centro = new Centro();
            centro.setCode(codigoCentroDefecto);
        }

        return centro;
    }


    /**
     * Obtiene el Repository de un expediente en función del id del expediente.
     * Se usa este método porque de otra forma se retornaría el Repositorio de Expediente y no del expdiente en concreto.
     *
     * @param idExpediente
     * @return
     */
    private JpaRepository<Expediente> getJpaRepository(long idExpediente) {
        JpaRepository<Expediente> onlyExpedienteRepository = JpaRepository.of(Expediente.class);
        Expediente expediente = onlyExpedienteRepository.find(idExpediente);
        EventManager eventManager = expediente.getTipoExpediente().getEventManager();
        JpaRepository<Expediente> realExpedienteRepository = JpaRepository.of(eventManager.getModelClass());
        JPA.em().detach(expediente);

        return realExpedienteRepository;
    }


}