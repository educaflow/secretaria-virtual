package com.educaflow.subsystem.tramitador.controller;

import com.educaflow.base.util.SecurityUtil;
import com.educaflow.subsystem.tramitador.tramitacion.util.ExpedienteUtil;
import com.educaflow.subsystem.security.service.PerfilesUsuarioService;
import org.apache.shiro.authz.UnauthorizedException;
import com.axelor.db.modelservice.ModelServiceFactory;
import com.axelor.db.JpaSecurity;
import com.axelor.db.Model;
import com.axelor.i18n.I18n;
import com.axelor.meta.CallMethod;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.axelor.rpc.Context;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.expedientes.db.Tramite;
import com.educaflow.subsystem.tramitador.service.TramitadorService;
import com.educaflow.subsystem.tramitador.service.VistaExpediente;
import com.educaflow.subsystem.tramitador.tramitacion.core.CommonEvent;
import com.educaflow.subsystem.tramitador.tramitacion.eventmanager.ContextoTramitacion;
import com.educaflow.subsystem.tramitador.tramitacion.eventmanager.EventContext;
import com.educaflow.subsystem.expedientes.db.Expediente;
import com.educaflow.subsystem.expedientes.db.Profile;
import com.educaflow.base.infrastructure.axelorhelper.ActionRequestHelper;
import com.educaflow.base.infrastructure.axelorhelper.ActionResponseHelper;
import com.educaflow.base.util.Convert;
import com.educaflow.base.infrastructure.validation.messages.BusinessException;
import com.axelor.db.modelservice.BusinessMessages;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.*;



public class TramitadorController {

    private static final Logger logger = LoggerFactory.getLogger(TramitadorController.class);

    @Inject
    TramitadorService tramitadorService;

    @Inject
    PerfilesUsuarioService perfilesUsuarioService;

    @Inject
    ModelServiceFactory modelServiceFactory;

    @Inject
    JpaSecurity jpaSecurity;


    public TramitadorController() {

    }

    @CallMethod
    @Transactional
    public void triggerInitialEvent(ActionRequest actionRequest, ActionResponse actionResponse) {
        ActionResponseHelper actionResponseHelper = new ActionResponseHelper(actionResponse);
        try {
            ContextoTramitacion contextoTramitacion = getContextoTramitacion(actionRequest.getContext());

            Optional<BusinessMessages> validacion = tramitadorService.validateTriggerInitialEvent(contextoTramitacion);
            if (validacion.isPresent()) {
                actionResponseHelper.doResponseBusinessMessagesAsError(I18n.get("No es posible crear el expediente"), validacion.get());
                return;
            }

            Expediente expediente = tramitadorService.triggerInitialEvent(contextoTramitacion);

            VistaExpediente vista = tramitadorService.getVistaExpediente(expediente, contextoTramitacion.profile());
            doResponseVistaExpediente(actionResponseHelper, vista);
            actionResponse.setCanClose(true);

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
    public void triggerEvent(ActionRequest actionRequest, ActionResponse actionResponse) {
        ActionRequestHelper actionRequestHelper = new ActionRequestHelper(actionRequest);
        ActionResponseHelper actionResponseHelper = new ActionResponseHelper(actionResponse);
        try {
            Expediente expediente = ExpedienteUtil.getExpedienteFromIdExpediente(actionRequestHelper.getId());
            String eventName = actionRequestHelper.getEventName();
            Map<String, Object> requestData = actionRequestHelper.getRequestData();
            Profile profile = Profile.valueOf(actionRequestHelper.getProfileName());
            EventContext eventContext = new EventContext(expediente, profile, modelServiceFactory);

            if (eventName.equals(CommonEvent.EXIT.name())) {
                actionResponse.setSignal("refresh-app", null);
                return;
            }

            exigeNoSerEventoDeSistema(expediente, eventName);

            Optional<BusinessMessages> validacion = tramitadorService.validateTriggerEvent(expediente, eventName, requestData, eventContext);
            if (validacion.isPresent()) {
                actionResponseHelper.doResponseBusinessMessagesAsError(I18n.get("No es posible tramitar el expediente"), validacion.get());
                return;
            }

            tramitadorService.triggerEvent(expediente, eventName, requestData, eventContext);

            if (eventName.equals(CommonEvent.DELETE.name())) {
                actionResponse.setSignal("refresh-app", null);
            } else {
                VistaExpediente vista = tramitadorService.getVistaExpediente(expediente, profile);
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
    public void viewExpediente(ActionRequest actionRequest, ActionResponse actionResponse) {
        ActionRequestHelper actionRequestHelper = new ActionRequestHelper(actionRequest);
        ActionResponseHelper actionResponseHelper = new ActionResponseHelper(actionResponse);
        try {
            Expediente expediente = ExpedienteUtil.getExpedienteFromIdExpediente(actionRequestHelper.getId());
            Profile profile = Profile.valueOf(actionRequestHelper.getProfileName());

            Optional<BusinessMessages> validacion = tramitadorService.validateGetVistaExpediente(expediente, profile);
            if (validacion.isPresent()) {
                actionResponseHelper.doResponseBusinessMessagesAsError(I18n.get("No es posible abrir el expediente"), validacion.get());
                return;
            }

            VistaExpediente vista = tramitadorService.getVistaExpediente(expediente, profile);
            doResponseVistaExpediente(actionResponseHelper, vista);
        } catch (UnauthorizedException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /************************************************************************************/
    /***************************** Acciones de Validaciones *****************************/
    /************************************************************************************/

    @CallMethod
    public void validateChild(ActionRequest actionRequest, ActionResponse actionResponse) {
        ActionRequestHelper actionRequestHelper = new ActionRequestHelper(actionRequest);
        ActionResponseHelper actionResponseHelper = new ActionResponseHelper(actionResponse);
        try {
            Expediente expediente = ExpedienteUtil.getExpedienteFromIdExpediente(actionRequestHelper.getParentId());
            Class<? extends Model> beanClass = actionRequestHelper.getModelClass();
            Map<String, Object> requestData = actionRequestHelper.getRequestData();

            Model bean = findModel(beanClass, actionRequestHelper.getId());
            String validateProperty = actionRequestHelper.getParentSource();

            Optional<BusinessMessages> validacion = tramitadorService.validateValidateChild(expediente, bean, beanClass, validateProperty, requestData);
            if (validacion.isPresent()) {
                actionResponseHelper.doResponseBusinessMessagesAsError(I18n.get("No es posible validar el expediente"), validacion.get());
                return;
            }

            BusinessMessages businessMessages = tramitadorService.validateChild(expediente, bean, beanClass, validateProperty, requestData);

            actionResponseHelper.doResponseBusinessMessages(businessMessages);

        } catch (UnauthorizedException ex) {
            // MUST NOT envolverla: sin envolver, ResponseInterceptor la reconoce como
            // AuthorizationException y responde un error de acceso (403) en vez de un 500 genérico.
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /****************************************************************************/
    /***************************** Métodos privados *****************************/
    /****************************************************************************/

    /**
     * Los eventos de sistema de un estado solo los dispara el servidor, llamando a
     * {@code TramitadorService.triggerEvent}. Por aquí pasa lo que llega en una petición, así que uno
     * de sistema solo puede ser alguien que lo manda a mano.
     */
    private static void exigeNoSerEventoDeSistema(Expediente expediente, String eventName) {
        if (ExpedienteUtil.getState(expediente).getSystemEvents().contains(eventName)) {
            throw new UnauthorizedException("El evento '" + eventName + "' es un evento de sistema y solo lo puede disparar el servidor");
        }
    }

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
     * <p>Lo normal es que una vista dispare el alta con la acción global del subsistema, declarada
     * en {@code subsystem/tramitador/controller/actions-tramitador.xml}, y que los cuatro datos
     * salgan de los campos del propio formulario. Así lo hace «Nuevo expediente»:
     * <pre>{@code
     * <action name="subsysTramitador-trigger-initial-event-action"/>
     * }</pre>
     *
     * <p>Una vista cuyo formulario <b>no</b> tenga esos campos declara su propia acción y los pasa
     * como contexto fijo:
     * <pre>{@code
     * <action-method name="...-Remote-triggerInitialEvent-action" model="...">
     *     <call class="com.educaflow.subsystem.tramitador.controller.TramitadorController" method="triggerInitialEvent"/>
     *     <context name="codeTramite" expr="eval: 'MI_TRAMITE'"/>
     *     <context name="codeCentro" expr="eval: '46012345'"/>
     *     <context name="presentadoEnPapel" expr="eval: true"/>
     *     <context name="presentadoEnRepresentacion" expr="eval: false"/>
     *     <context name="idioma" expr="es"/>
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
        String idioma = (String) context.get("idioma");

        Profile profile = perfilesUsuarioService.getPerfil(tramite, SecurityUtil.getUser(), centro, presentadoEnPapel);

        return new ContextoTramitacion(tramite, centro, profile, presentadoEnPapel, presentadoEnRepresentacion, idioma);
    }

    private Tramite getTramite(Context context) {
        String code = getCode(context, "codeTramite");

        return code != null
                ? modelServiceFactory.resolve(Tramite.class).getByCode(code)
                : getReferencia(context, "tramite", Tramite.class);
    }

    private Centro getCentro(Context context) {
        String code = getCode(context, "codeCentro");

        return code != null
                ? modelServiceFactory.resolve(Centro.class).getByCode(code)
                : getReferencia(context, "centro", Centro.class);
    }

    private String getCode(Context context, String fieldName) {
        Object code = context.get(fieldName);

        if (code == null || code.toString().isBlank()) {
            return null;
        }

        return code.toString();
    }

    private <T extends Model> T getReferencia(Context context, String fieldName, Class<T> modelClass) {
        Model referencia = (Model) context.get(fieldName);

        if (referencia == null || referencia.getId() == null) {
            return null;
        }

        return modelServiceFactory.resolve(modelClass).getById(referencia.getId());
    }

    private <T extends Model> T findModel(Class<T> classModel, Long id) {
        // Solo comprueba el permiso de lectura sobre la clase, NO que el hijo pertenezca al expediente
        // (pendiente). Va fuera del try para que el catch (Exception) no envuelva la UnauthorizedException.
        // TODO: No verifica que ese hijo pertenezca al expediente que viene como padre en la petición, así que un usuario con permiso de lectura sobre la clase puede seguir pasando el id de un hijo de otro expediente.
        if (id != null) {
            logger.warn("TODO:Comprobar que tiene permiso para esa fila!!!!!");
            jpaSecurity.check(JpaSecurity.CAN_READ, classModel, Convert.objectToLong(id));
        }

        try {
            if (id == null) {
                return classModel.getConstructor().newInstance();
            }

            return modelServiceFactory.resolve(classModel).getById(Convert.objectToLong(id));
        } catch (Exception ex) {
            throw new RuntimeException("Error al encontrar el modelo: " + classModel.getName() + " con id: " + id, ex);
        }
    }

}
