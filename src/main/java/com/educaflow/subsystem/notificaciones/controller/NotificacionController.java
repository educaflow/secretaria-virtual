package com.educaflow.subsystem.notificaciones.controller;

import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.ModelServiceFactory;
import com.axelor.i18n.I18n;
import com.axelor.meta.CallMethod;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.educaflow.base.infrastructure.axelorhelper.ActionRequestHelper;
import com.educaflow.base.infrastructure.axelorhelper.ActionResponseHelper;
import com.educaflow.subsystem.notificaciones.db.Notificacion;
import com.educaflow.subsystem.notificaciones.db.TipoNotificacion;
import com.educaflow.subsystem.notificaciones.service.NotificacionService;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;

import java.util.Optional;

public class NotificacionController {

    @Inject
    private ModelServiceFactory modelServiceFactory;

    @CallMethod
    public void abrirTodas(ActionRequest actionRequest, ActionResponse actionResponse) {
        abrirEnPopup(actionRequest, actionResponse, "Main");
    }

    @CallMethod
    public void abrirDelCentro(ActionRequest actionRequest, ActionResponse actionResponse) {
        abrirEnPopup(actionRequest, actionResponse, "Centro");
    }

    @CallMethod
    public void abrirRecibida(ActionRequest actionRequest, ActionResponse actionResponse) {
        abrirEnPopup(actionRequest, actionResponse, "Mis");
    }

    @CallMethod
    public void continuarAlta(ActionRequest actionRequest, ActionResponse actionResponse) {
        TipoNotificacion tipoNotificacion = tipoDelContexto(actionRequest);
        Class<? extends Notificacion> claseNotificacion = tipoNotificacion.getClaseNotificacion();

        new ActionResponseHelper(actionResponse).doResponseViewFormEnPopup(nombreDelForm("Main", claseNotificacion), claseNotificacion, null, I18n.get("Notificación"));
    }

    @CallMethod
    public void prepararAlta(ActionRequest actionRequest, ActionResponse actionResponse) {
        // Solo presentación: el tipo que cuenta lo vuelve a asignar el servicio al guardar.
        actionResponse.setValue("tipoNotificacion", TipoNotificacion.deClase(new ActionRequestHelper<>(actionRequest).getModelClass()));
    }

    @CallMethod
    @Transactional
    public void reenviar(ActionRequest actionRequest, ActionResponse actionResponse) {
        Class<Notificacion> claseNotificacion = claseDelCanal(actionRequest);
        NotificacionService<Notificacion> canalService = servicioDelCanal(claseNotificacion);
        ActionRequestHelper<Notificacion> actionRequestHelper = new ActionRequestHelper<>(actionRequest, claseNotificacion);

        Notificacion entidadOriginal = actionRequestHelper.getOriginalModel();
        Notificacion entidad = actionRequestHelper.getModel(canalService.allowPropertiesReenviar());

        canalService.reenviar(entidad, entidadOriginal);

        actionResponse.setNotify(I18n.get("El reenvío de la notificación se ha puesto en marcha."));
        actionResponse.setReload(true);
    }

    /**************************************************************************************/
    /****************************** Acciones de Validaciones ******************************/
    /**************************************************************************************/

    @CallMethod
    public void validateReenviar(ActionRequest actionRequest, ActionResponse actionResponse) {
        Class<Notificacion> claseNotificacion = claseDelCanal(actionRequest);
        NotificacionService<Notificacion> canalService = servicioDelCanal(claseNotificacion);
        ActionRequestHelper<Notificacion> actionRequestHelper = new ActionRequestHelper<>(actionRequest, claseNotificacion);

        Notificacion entidadOriginal = actionRequestHelper.getOriginalModel();
        Notificacion entidad = actionRequestHelper.getModel(canalService.allowPropertiesReenviar());

        Optional<BusinessMessages> validationResult = canalService.validateReenviar(entidad, entidadOriginal);
        if (validationResult.isPresent()) {
            new ActionResponseHelper(actionResponse).doResponseBusinessMessagesAsError(validationResult.get());
        }
    }

    /******************************************************************************/
    /****************************** Métodos privados ******************************/
    /******************************************************************************/

    private void abrirEnPopup(ActionRequest actionRequest, ActionResponse actionResponse, String variante) {
        TipoNotificacion tipoNotificacion = tipoDelContexto(actionRequest);
        Class<? extends Notificacion> claseNotificacion = tipoNotificacion.getClaseNotificacion();
        Long id = new ActionRequestHelper<>(actionRequest).getId();

        // No se carga la fila: el popup la pide por REST, donde Axelor aplica los permisos del usuario.
        new ActionResponseHelper(actionResponse).doResponseViewFormEnPopup(nombreDelForm(variante, claseNotificacion), claseNotificacion, id, I18n.get("Notificación"));
    }

    private static TipoNotificacion tipoDelContexto(ActionRequest actionRequest) {
        // Del mapa de la petición y no de Context.get, que construye el bean: en la Notificacion base el destino calculado lanza.
        Object tipoNotificacion = new ActionRequestHelper<>(actionRequest).getRequestData().get("tipoNotificacion");

        if (tipoNotificacion == null) {
            throw new IllegalStateException("La petición no trae el tipoNotificacion de la notificación");
        }

        return TipoNotificacion.valueOf(tipoNotificacion.toString());
    }

    private NotificacionService<Notificacion> servicioDelCanal(Class<Notificacion> claseNotificacion) {
        // Todo canal tiene su <Canal>Service, que extiende NotificacionService.
        return (NotificacionService<Notificacion>) modelServiceFactory.resolve(claseNotificacion);
    }

    @SuppressWarnings("unchecked")
    private static Class<Notificacion> claseDelCanal(ActionRequest actionRequest) {
        // El _model del form abierto es la subclase del canal; deClase rechaza la que no es de ninguno.
        // Se ve como Class<Notificacion> para tratar a todos los canales igual: entidad y servicio son siempre de esa misma clase.
        return (Class<Notificacion>) TipoNotificacion.deClase(new ActionRequestHelper<>(actionRequest).getModelClass()).getClaseNotificacion();
    }

    private static String nombreDelForm(String variante, Class<? extends Notificacion> claseNotificacion) {
        return "subsysNotificaciones." + variante + "@" + claseNotificacion.getSimpleName() + "-form";
    }

}
