package com.educaflow.subsystem.expedientes.controllers;

import com.educaflow.base.util.SecurityUtil;
import com.educaflow.subsystem.expedientes.tramitacion.util.ExpedienteUtil;
import com.educaflow.subsystem.security.service.PerfilesUsuarioService;
import org.apache.shiro.authz.UnauthorizedException;
import com.axelor.db.JpaRepository;
import com.axelor.db.modelservice.ModelServiceFactory;
import com.axelor.db.Model;
import com.axelor.i18n.I18n;
import com.axelor.meta.CallMethod;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.axelor.rpc.Context;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.expedientes.db.Tramite;
import com.educaflow.subsystem.expedientes.services.ExpedienteService;
import com.educaflow.subsystem.expedientes.services.VistaExpediente;
import com.educaflow.subsystem.expedientes.tramitacion.internal.ExpedienteLocator;
import com.educaflow.subsystem.expedientes.tramitacion.core.CommonEvent;
import com.educaflow.subsystem.expedientes.tramitacion.eventmanager.ContextoTramitacion;
import com.educaflow.subsystem.expedientes.tramitacion.eventmanager.EventContext;
import com.educaflow.subsystem.expedientes.db.Expediente;
import com.educaflow.subsystem.expedientes.db.Profile;
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
    PerfilesUsuarioService perfilesUsuarioService;

    @Inject
    ModelServiceFactory modelServiceFactory;


    public ExpedienteController() {

    }

    @CallMethod
    @Transactional
    public void triggerInitialEvent(ActionRequest actionRequest, ActionResponse response) {
        ActionResponseHelper actionResponseHelper = new ActionResponseHelper(response);
        try {
            ContextoTramitacion contextoTramitacion = getContextoTramitacion(actionRequest.getContext());

            Expediente expediente = expedienteService.triggerInitialEvent(contextoTramitacion);

            VistaExpediente vista = expedienteService.getVistaExpediente(expediente, contextoTramitacion.profile());
            doResponseVistaExpediente(actionResponseHelper, vista);
            response.setCanClose(true);

        } catch (BusinessException ex) {
            actionResponseHelper.doResponseBusinessMessagesAsError(I18n.get("No es posible crear el expediente"), ex.getBusinessMessages());
        } catch (UnauthorizedException ex) {
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
            Profile profile = Profile.valueOf(actionRequestHelper.getProfileName());
            EventContext eventContext = new EventContext(expediente, profile, modelServiceFactory);

            if (eventName.equals(CommonEvent.EXIT.name())) {
                response.setSignal("refresh-app", null);
                return;
            }

            expedienteService.triggerEvent(expediente, eventName, requestData, eventContext);

            if (eventName.equals(CommonEvent.DELETE.name())) {
                response.setSignal("refresh-app", null);
            } else {
                VistaExpediente vista = expedienteService.getVistaExpediente(expediente, profile);
                doResponseVistaExpediente(actionResponseHelper, vista);
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
            Profile profile = Profile.valueOf(actionRequestHelper.getProfileName());

            VistaExpediente vista = expedienteService.getVistaExpediente(expediente, profile);
            doResponseVistaExpediente(actionResponseHelper, vista);
        } catch (UnauthorizedException ex) {
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


    private void doResponseVistaExpediente(ActionResponseHelper actionResponseHelper, VistaExpediente vista) {
        actionResponseHelper.doResponseViewForm(vista.viewName(), vista.modelClass(), vista.expediente(), vista.title(), vista.profile().name());
    }

    /**
     * El contexto del alta que trae la petición del cliente: solo los campos que el alta necesita. El
     * perfil no se lee del cliente, sale de los perfiles de inicio del usuario.
     *
     * <p>El trámite y el centro llegan como referencias del formulario ({@code tramite}, {@code
     * centro}) o como códigos sueltos ({@code codeTramite}, {@code codeCentro}), para que cualquier
     * vista pueda crear un expediente sin pasar por «Nuevo expediente»; si viene el código, manda. Las
     * referencias se vuelven a buscar por id porque {@code Context} las devuelve desligadas de la
     * sesión.
     *
     * <p>Una vista cualquiera dispara el alta así:
     * <pre>{@code
     * <action-method name="...-Remote-triggerInitialEvent-action" model="...">
     *     <call class="com.educaflow.subsystem.expedientes.controllers.ExpedienteController" method="triggerInitialEvent"/>
     *     <context name="codeTramite" expr="eval: 'MI_TRAMITE'"/>
     *     <context name="codeCentro" expr="eval: '46012345'"/>
     *     <context name="presentadoEnPapel" expr="eval: true"/>
     *     <context name="presentadoEnRepresentacion" expr="eval: false"/>
     * </action-method>
     * }</pre>
     * Los booleanos <b>MUST</b> llevar {@code eval:}: sin él llegan como {@code String} y se
     * interpretan como {@code false}.
     */
    private ContextoTramitacion getContextoTramitacion(Context context) {
        Tramite tramite = getTramite(context);
        Centro centro = getCentro(context);
        boolean presentadoEnPapel = Boolean.TRUE.equals(context.get("presentadoEnPapel"));
        boolean presentadoEnRepresentacion = Boolean.TRUE.equals(context.get("presentadoEnRepresentacion"));

        Profile profile = perfilesUsuarioService.getPerfil(tramite, SecurityUtil.getUser(), centro, presentadoEnPapel);

        return new ContextoTramitacion(tramite, centro, profile, presentadoEnPapel, presentadoEnRepresentacion);
    }

    private Tramite getTramite(Context context) {
        String code = getCode(context, "codeTramite");

        return code != null ? getPorCode(Tramite.class, code) : getReferencia(context, "tramite", Tramite.class);
    }

    private Centro getCentro(Context context) {
        String code = getCode(context, "codeCentro");

        return code != null ? getPorCode(Centro.class, code) : getReferencia(context, "centro", Centro.class);
    }

    private String getCode(Context context, String fieldName) {
        Object code = context.get(fieldName);

        if (code == null || code.toString().isBlank()) {
            return null;
        }

        return code.toString();
    }


    /*******************************************************************/
    /*************** Funciones de Acceso a Base de datos ***************/
    /*******************************************************************/


    /**
     * El código lo pone la vista que dispara el alta, no el usuario: que no exista es un error de
     * programación de esa vista, no algo que el usuario de la pantalla pueda corregir.
     */
    private <T extends Model> T getPorCode(Class<T> modelClass, String code) {
        T model = JpaRepository.of(modelClass).all()
                .filter("self.code = :code")
                .bind("code", code)
                .fetchOne();

        if (model == null) {
            throw new IllegalArgumentException("No existe ningún " + modelClass.getSimpleName() + " con el código: " + code);
        }

        return model;
    }

    private <T extends Model> T getReferencia(Context context, String fieldName, Class<T> modelClass) {
        Model referencia = (Model) context.get(fieldName);

        if (referencia == null || referencia.getId() == null) {
            return null;
        }

        return JpaRepository.of(modelClass).find(referencia.getId());
    }

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
