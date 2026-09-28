package com.educaflow.system.ventanilla.controller;

import com.axelor.auth.db.User;
import com.axelor.db.JPA;
import com.axelor.meta.CallMethod;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.educaflow.base.infrastructure.axelorhelper.ActionRequestHelper;
import com.educaflow.base.infrastructure.axelorhelper.ActionResponseHelper;
import com.educaflow.base.util.SecurityUtil;
import com.educaflow.subsystem.expedientes.db.Expediente;
import com.educaflow.subsystem.expedientes.db.Profile;
import com.educaflow.subsystem.security.service.PerfilesUsuarioService;
import com.educaflow.subsystem.tramitador.service.TramitadorService;
import com.educaflow.subsystem.tramitador.service.VistaExpediente;
import com.educaflow.subsystem.tramitador.tramitacion.util.ExpedienteUtil;
import jakarta.inject.Inject;
import org.apache.shiro.authz.UnauthorizedException;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Lo que las bandejas de expedientes de la ventanilla piden al servidor: qué expedientes «me tocan» y
 * con qué perfil se abre el que se pulsa.
 *
 * <p>Responde <b>siempre</b> sobre el usuario autenticado ({@link SecurityUtil#getUser()}): no recibe
 * usuario ni centros, así que quien llama solo obtiene lo que ya puede ver.
 *
 * <p>Quién ostenta qué perfil sobre un expediente <b>no</b> se reescribe aquí en JPQL: esa pregunta
 * tiene dueño, {@link PerfilesUsuarioService#getPerfilesSobreExpediente}, y este controlador le
 * pregunta una vez por expediente candidato. El conjunto candidato está acotado por construcción
 * (los expedientes abiertos de los centros del usuario cuyo estado espera a alguien que no es su
 * creador), así que es del tamaño de la propia bandeja; si ese coste llegara a molestar, el arreglo
 * va en el dueño (un método masivo en el servicio o en los repositorios {@code AceProfile*}), nunca
 * devolviendo el JPQL de perfiles a las vistas.
 *
 * <p>El JPQL que acota los candidatos sí vive aquí, y no en un repositorio, a propósito: su dueño
 * natural sería {@code subsystem/expedientes}, pero llevarlo allí obligaría a ampliar el motor de
 * tramitación, que debe mantenerse lo más pequeño posible; y colgar un repositorio de
 * {@link Expediente} de un sistema rompería que el repositorio es un detalle interno del subsistema
 * dueño de la entidad.
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
    PerfilesUsuarioService perfilesUsuarioService;

    @Inject
    TramitadorService tramitadorService;

    /**
     * Los ids de los expedientes abiertos de los centros del usuario autenticado que están esperando
     * a que actúe él <b>como tramitador</b>: el perfil que declara su estado actual no es
     * {@code CREADOR} y el usuario lo ostenta sobre ese expediente. Es la misma regla con la que el
     * tramitador autoriza el evento ({@code validatePerfilDelEstado}), así que la bandeja y la
     * autorización dicen lo mismo por construcción.
     *
     * <p>El lado del ciudadano (lo que me toca como {@code CREADOR}) no pasa por aquí: se resuelve en
     * JPQL puro con {@code usuarioRegistrador} y {@code perfilEstado}.
     */
    @CallMethod
    public List<Long> idsPendientesDeMi() {
        User user = SecurityUtil.getUser();
        if (user == null) {
            return NINGUNO;
        }

        List<Long> ids = JPA.all(Expediente.class)
                .filter("self.abierto = true"
                        + " AND self.perfilEstado IS NOT NULL AND self.perfilEstado <> :creador"
                        + " AND self.centro IN (SELECT cu.centro FROM CentroUsuario cu WHERE cu.usuario = :usuario)")
                .bind("creador", Profile.CREADOR)
                .bind("usuario", user)
                .fetch()
                .stream()
                .filter(expediente -> perfilesSobreExpediente(expediente, user).contains(expediente.getPerfilEstado()))
                .map(Expediente::getId)
                .toList();

        return ids.isEmpty() ? NINGUNO : ids;
    }

    @CallMethod
    public void abrirExpediente(ActionRequest actionRequest, ActionResponse actionResponse) {
        ActionRequestHelper<Expediente> actionRequestHelper = new ActionRequestHelper<>(actionRequest, Expediente.class);
        ActionResponseHelper actionResponseHelper = new ActionResponseHelper(actionResponse);
        try {
            Expediente expediente = ExpedienteUtil.getExpedienteFromIdExpediente(actionRequestHelper.getId());
            Profile perfil = perfilConElQueAbrir(expediente, SecurityUtil.getUser());

            VistaExpediente vista = tramitadorService.getVistaExpediente(expediente, perfil);
            actionResponseHelper.doResponseViewForm(vista.viewName(), vista.modelClass(), vista.expediente(), vista.title(), vista.profile().name());
        } catch (UnauthorizedException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private Profile perfilConElQueAbrir(Expediente expediente, User user) {
        Profile perfil;
        Set<Profile> perfiles = perfilesSobreExpediente(expediente, user);

        Profile perfilDelEstado = expediente.getPerfilEstado();
        if (perfilDelEstado != null && perfiles.contains(perfilDelEstado)) {
            perfil=perfilDelEstado;
        } else {
            perfil = perfiles.stream()
                    .max(Comparator.comparingInt(Profile::getPrioridad))
                    .orElseThrow(() -> new UnauthorizedException("El usuario no tiene perfil sobre el expediente " + expediente.getId()));
        }

        return perfil;
    }

    private Set<Profile> perfilesSobreExpediente(Expediente expediente, User user) {
        if (SecurityUtil.isAdmin(user)) {
            return Set.of(Profile.values());
        }
        return perfilesUsuarioService.getPerfilesSobreExpediente(expediente, user);
    }
}
