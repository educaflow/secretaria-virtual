package com.educaflow.subsystem.security.service.impl;

import com.axelor.auth.db.User;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.expedientes.db.Expediente;
import com.educaflow.subsystem.expedientes.db.Tramite;
import com.educaflow.subsystem.security.db.repo.AceRepository;
import com.educaflow.subsystem.security.service.PerfilesUsuarioService;
import jakarta.inject.Inject;

import java.util.LinkedHashSet;
import java.util.Set;

public class PerfilesUsuarioServiceImpl implements PerfilesUsuarioService {

    /** El perfil del creador del expediente, que no se asigna con un {@code Ace} sino por autoría. */
    private static final String PERFIL_CREADOR = "CREADOR";

    @Inject
    AceRepository aceRepository;

    @Override
    public Set<String> getPerfilesSobreExpediente(Expediente expediente, User user) {
        if ((expediente == null) || (user == null)) {
            return Set.of();
        }

        Centro centro = expediente.getCentro();
        Set<String> perfiles = new LinkedHashSet<>(aceRepository.findNombresPerfilesByExpediente(
                expediente, centro, user.getCentroUsuario(centro)));

        // El creador no tiene fila Ace: lo es por haber registrado el expediente. Es la misma
        // condición que el permiso Expediente.creador de auth-expedientes.xml.
        if (esCreador(expediente, user)) {
            perfiles.add(PERFIL_CREADOR);
        }

        return perfiles;
    }

    @Override
    public Set<String> getPerfilesSobreTramite(Tramite tramite, User user, Centro centro) {
        if ((tramite == null) || (user == null) || (centro == null)) {
            return Set.of();
        }

        return aceRepository.findNombresPerfilesByTramite(tramite, centro, user.getCentroUsuario(centro));
    }

    private static boolean esCreador(Expediente expediente, User user) {
        User usuarioRegistrador = expediente.getUsuarioRegistrador();

        return (usuarioRegistrador != null)
                && (usuarioRegistrador.getId() != null)
                && usuarioRegistrador.getId().equals(user.getId());
    }

}
