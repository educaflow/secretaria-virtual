package com.educaflow.subsystem.expedientes.controllers;

import com.axelor.auth.db.User;
import com.axelor.db.JpaRepository;
import com.axelor.i18n.I18n;
import com.axelor.meta.CallMethod;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.educaflow.base.util.SecurityUtil;
import com.educaflow.subsystem.expedientes.services.ContextoTramitacionService;
import com.educaflow.subsystem.expedientes.db.Tramite;
import com.educaflow.subsystem.expedientes.db.repo.TramiteRepository;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.base.infrastructure.axelorhelper.ActionRequestHelper;
import com.educaflow.base.util.Convert;
import com.google.inject.Inject;


import java.util.*;
import java.util.stream.Collectors;



public class ContextoTramitacionController {

    @Inject
    TramiteRepository tramiteRepository;

    @Inject
    ContextoTramitacionService contextoTramitacionService;


    public ContextoTramitacionController() {

    }

    @CallMethod
    public void prepararContextoTramitacion(ActionRequest request, ActionResponse response) {
        ActionRequestHelper actionRequestHelper = new ActionRequestHelper(request);
        User user = SecurityUtil.getUser();
        Tramite tramite = getTramite(Convert.objectToLong(actionRequestHelper.getRequestData().get("_tramiteId")));

        response.setValue("tramite", Map.of("id", tramite.getId(), "name", tramite.getName()));
        response.setValue("nombreTramite", I18n.get(tramite.getName()));
        response.setValue("ayudaTramite", tramite.getHelp());

        List<Centro> centros = contextoTramitacionService.getCentros(tramite, user);
        if (centros.isEmpty()) {
            response.setError(I18n.get("No puede crear expedientes de este trámite en ninguno de sus centros"));
            return;
        }

        String idsCentros = centros.stream().map(centro -> String.valueOf(centro.getId())).collect(Collectors.joining(","));
        response.setAttr("centro", "domain", "self.id IN (" + idsCentros + ")");

        Centro centro = null;
        if (centros.size() == 1) {
            centro = centros.get(0);
            response.setValue("centro", Map.of("id", centro.getId(), "name", centro.getName()));
            response.setAttr("centro", "readonly", true);
        }

        boolean presentadoEnPapel = responderPresentadoEnPapel(tramite, user, centro, response);
        responderOpcionesParaQuien(tramite, user, centro, presentadoEnPapel, response);
    }

    @CallMethod
    public void cambiarCentroContextoTramitacion(ActionRequest request, ActionResponse response) {
        ActionRequestHelper actionRequestHelper = new ActionRequestHelper(request);
        Map<String, Object> requestData = actionRequestHelper.getRequestData();
        Tramite tramite = getTramite(Convert.objectToLong(requestData.get("_tramiteId")));
        Centro centro = getCentro(requestData.get("centro"));

        User user = SecurityUtil.getUser();

        response.setValue("presentadoEnRepresentacion", null);
        boolean presentadoEnPapel = responderPresentadoEnPapel(tramite, user, centro, response);
        responderOpcionesParaQuien(tramite, user, centro, presentadoEnPapel, response);
    }

    @CallMethod
    public void cambiarPresentadoEnPapelContextoTramitacion(ActionRequest request, ActionResponse response) {
        ActionRequestHelper actionRequestHelper = new ActionRequestHelper(request);
        Map<String, Object> requestData = actionRequestHelper.getRequestData();
        Tramite tramite = getTramite(Convert.objectToLong(requestData.get("_tramiteId")));
        Centro centro = getCentro(requestData.get("centro"));
        boolean presentadoEnPapel = Boolean.TRUE.equals(requestData.get("presentadoEnPapel"));

        User user = SecurityUtil.getUser();

        response.setValue("presentadoEnRepresentacion", null);
        responderProfile(presentadoEnPapel, response);
        responderOpcionesParaQuien(tramite, user, centro, presentadoEnPapel, response);
    }


    /*******************************************************************/
    /*************** Funciones de Acceso a Base de datos ***************/
    /*******************************************************************/

    private Tramite getTramite(Long idTramite) {
        if (idTramite == null) {
            throw new RuntimeException("No se ha indicado el trámite");
        }
        Tramite tramite = tramiteRepository.find(idTramite);
        if (tramite == null) {
            throw new RuntimeException("No existe el tramite con idTramite: " + idTramite);
        }
        return tramite;
    }

    private static Centro getCentro(Object centroRequest) {
        if (centroRequest == null) {
            return null;
        }
        if (!(centroRequest instanceof Map<?, ?> centroMap)) {
            throw new RuntimeException("El centro de la petición no tiene el formato esperado: " + centroRequest.getClass());
        }

        Object idCentro = centroMap.get("id");
        if (idCentro == null) {
            return null;
        }

        return JpaRepository.of(Centro.class).find(Convert.objectToLong(idCentro));
    }


    /*******************************************************************/
    /********************** Funciones de Utilidad **********************/
    /*******************************************************************/

    /**
     * Cómo presenta el usuario. Solo se le pregunta a quien tiene los dos perfiles, porque es el único
     * que puede actuar de las dos formas; al resto se le deduce de su único perfil y no se le pregunta
     * nada. Devuelve el valor que queda en el formulario.
     */
    private boolean responderPresentadoEnPapel(Tramite tramite, User user, Centro centro, ActionResponse response) {
        boolean esNecesarioPresentadoEnPapel = contextoTramitacionService.esNecesarioPresentadoEnPapel(tramite, user, centro);
        boolean presentadoEnPapel = esNecesarioPresentadoEnPapel
                //Con las dos opciones se pregunta: el checkbox arranca sin marcar, presentando como creador.
                ? false
                : contextoTramitacionService.deducirPresentadoEnPapel(tramite, user, centro);

        response.setValue("presentadoEnPapel", presentadoEnPapel);
        response.setAttr("presentadoEnPapel", "hidden", esNecesarioPresentadoEnPapel == false);
        responderProfile(presentadoEnPapel, response);

        return presentadoEnPapel;
    }

    /**
     * El perfil con el que se abre el expediente: el ContextoTramitacion viaja al alta y de él sale el
     * EventContext, así que MUST ir siempre en sintonía con presentadoEnPapel.
     */
    private void responderProfile(boolean presentadoEnPapel, ActionResponse response) {
        response.setValue("profile", contextoTramitacionService.getProfile(presentadoEnPapel));
    }

    private void responderOpcionesParaQuien(Tramite tramite, User user, Centro centro, boolean presentadoEnPapel, ActionResponse response) {
        response.setAttr("presentadoEnRepresentacion", "title", presentadoEnPapel
                ? I18n.get("¿Para quién es la solicitud? («Para mí» es para la persona que la ha entregado)")
                : I18n.get("¿Para quién es el expediente?"));

        if (centro == null) {
            response.setAttr("presentadoEnRepresentacion", "hidden", true);
            return;
        }

        boolean paraMi = contextoTramitacionService.puedeCrear(tramite, user, centro, presentadoEnPapel, false);
        boolean paraOtraPersona = contextoTramitacionService.puedeCrear(tramite, user, centro, presentadoEnPapel, true);

        if ((paraMi || paraOtraPersona) == false) {
            response.setAttr("presentadoEnRepresentacion", "hidden", true);
        } else if (paraMi != paraOtraPersona) {
            //Solo cabe una opción: se asigna sin preguntar.
            response.setValue("presentadoEnRepresentacion", paraOtraPersona);
            response.setAttr("presentadoEnRepresentacion", "hidden", true);
        } else {
            //Con las dos opciones hay que elegir: un booleano puede venir precargado a false y el required no obligaría.
            response.setValue("presentadoEnRepresentacion", null);
            response.setAttr("presentadoEnRepresentacion", "hidden", false);
        }
    }

}
