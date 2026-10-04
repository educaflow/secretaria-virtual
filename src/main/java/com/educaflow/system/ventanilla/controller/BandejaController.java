package com.educaflow.system.ventanilla.controller;

import com.axelor.auth.db.User;
import com.axelor.meta.CallMethod;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.educaflow.base.infrastructure.axelorhelper.ActionRequestHelper;
import com.educaflow.base.infrastructure.axelorhelper.ActionResponseHelper;
import com.educaflow.base.util.SecurityUtil;
import com.educaflow.subsystem.expedientes.db.Expediente;
import com.educaflow.subsystem.expedientes.db.Profile;
import com.educaflow.subsystem.tramitador.service.TramitadorService;
import com.educaflow.subsystem.tramitador.service.VistaExpediente;
import com.educaflow.subsystem.tramitador.tramitacion.util.ExpedienteUtil;
import com.educaflow.system.ventanilla.service.BandejaService;
import jakarta.inject.Inject;
import org.apache.shiro.authz.UnauthorizedException;

import java.util.List;

/**
 * Lo que las bandejas de expedientes de la ventanilla piden al servidor: qué expedientes «me tocan» y
 * con qué perfil se abre el que se pulsa. Las dos preguntas las responde {@link BandejaService}.
 *
 * <p>Responde <b>siempre</b> sobre el usuario autenticado ({@link SecurityUtil#getUser()}): no recibe
 * usuario ni centros, así que quien llama solo obtiene lo que ya puede ver.
 *
 * <p><b>Esta pieza no autoriza nada.</b> Los ids viajan al cliente dentro del {@code <context>} de la
 * {@code <action-view>} y vuelven del navegador en cada búsqueda, así que son manipulables. Es un
 * filtro de <b>interfaz</b>, que la bandeja enseñe lo que toca, y no un control de acceso: qué puede
 * leer cada usuario lo deciden los permisos de {@code auth-expedientes.xml}, y quién puede actuar
 * lo decide el tramitador al disparar cada evento.
 */
public class BandejaController {

    /**
     * Lo que se devuelve cuando no hay nada que listar: {@code self.id IN (:lista)} con la lista
     * vacía no es portable, y {@code -1} no es el id de ninguna fila.
     */
    private static final List<Long> NINGUNO = List.of(-1L);

    @Inject
    BandejaService bandejaService;

    @Inject
    TramitadorService tramitadorService;

    /**
     * El lado del ciudadano (lo que me toca como {@code CREADOR}) no pasa por aquí: se resuelve
     * en JPQL puro con {@code usuarioRegistrador} y {@code perfilEstado}.
     */
    @CallMethod
    public List<Long> idsPendientesDeMi() {
        User user = SecurityUtil.getUser();
        if (user == null) {
            return NINGUNO;
        }

        List<Long> ids = bandejaService.idsPendientesDeMi(user);
        return ids.isEmpty() ? NINGUNO : ids;
    }

    @CallMethod
    public void abrirExpediente(ActionRequest actionRequest, ActionResponse actionResponse) {
        ActionRequestHelper<Expediente> actionRequestHelper = new ActionRequestHelper<>(actionRequest, Expediente.class);
        ActionResponseHelper actionResponseHelper = new ActionResponseHelper(actionResponse);
        try {
            Expediente expediente = ExpedienteUtil.getExpedienteFromIdExpediente(actionRequestHelper.getId());
            Profile perfil = bandejaService.perfilConElQueAbrir(expediente, SecurityUtil.getUser());

            VistaExpediente vista = tramitadorService.getVistaExpediente(expediente, perfil);
            actionResponseHelper.doResponseViewForm(vista.viewName(), vista.modelClass(), vista.expediente(), vista.title(), vista.profile().name());
        } catch (UnauthorizedException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
