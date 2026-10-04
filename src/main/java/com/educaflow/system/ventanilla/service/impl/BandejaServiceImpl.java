package com.educaflow.system.ventanilla.service.impl;

import com.axelor.auth.db.User;
import com.axelor.db.JPA;
import com.educaflow.base.util.SecurityUtil;
import com.educaflow.subsystem.expedientes.db.Expediente;
import com.educaflow.subsystem.expedientes.db.Profile;
import com.educaflow.subsystem.security.service.PerfilesUsuarioService;
import com.educaflow.system.ventanilla.service.BandejaService;
import jakarta.inject.Inject;
import org.apache.shiro.authz.UnauthorizedException;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class BandejaServiceImpl implements BandejaService {

    private final PerfilesUsuarioService perfilesUsuarioService;

    @Inject
    public BandejaServiceImpl(PerfilesUsuarioService perfilesUsuarioService) {
        this.perfilesUsuarioService = perfilesUsuarioService;
    }

    /**
     * Es la misma regla con la que el tramitador autoriza el evento
     * ({@code TramitadorService.validatePerfilDelEstado}), escrita aparte: si cambia una, hay que cambiar la otra.
     */
    @Override
    public List<Long> idsPendientesDeMi(User user) {
        return JPA.all(Expediente.class)
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
    }

    private Set<Profile> perfilesSobreExpediente(Expediente expediente, User user) {
        if (SecurityUtil.isAdmin(user)) {
            return Set.of(Profile.values());
        }
        return perfilesUsuarioService.getPerfilesSobreExpediente(expediente, user);
    }

    @Override
    public Profile perfilConElQueAbrir(Expediente expediente, User user) {
        Set<Profile> perfiles = perfilesSobreExpediente(expediente, user);

        Profile perfilDelEstado = expediente.getPerfilEstado();
        if (perfilDelEstado != null && perfiles.contains(perfilDelEstado)) {
            return perfilDelEstado;
        }

        return perfiles.stream()
                .max(Comparator.comparingInt(Profile::getPrioridad))
                .orElseThrow(() -> new UnauthorizedException("El usuario no tiene perfil sobre el expediente " + expediente.getId()));
    }
}
