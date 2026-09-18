package com.educaflow.subsystem.expedientes.services;

import com.axelor.auth.db.User;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.common.db.CentroUsuario;
import com.educaflow.subsystem.expedientes.db.Profile;
import com.educaflow.subsystem.expedientes.db.Tramite;
import com.educaflow.subsystem.security.service.PerfilesUsuarioService;
import com.google.inject.Inject;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;


public class ContextoTramitacionService {

    @Inject
    PerfilesUsuarioService perfilesUsuarioService;

    /**
     * Los centros del usuario en los que puede crear algún expediente del trámite, ordenados por
     * nombre. Un centro aparece si en él tiene sobre el trámite el perfil de creador o el de tramitador.
     */
    public List<Centro> getCentros(Tramite tramite, User user) {
        if ((tramite == null) || (user == null) || (user.getCentroUsuarios() == null)) {
            return List.of();
        }

        return user.getCentroUsuarios().stream()
                .map(CentroUsuario::getCentro)
                .filter(Objects::nonNull)
                .filter(centro -> tienePerfilParaCrear(tramite, user, centro))
                .sorted(Comparator.comparing(Centro::getName, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    public boolean permitidoPresentarEnRepresentacion(Tramite tramite) {
        return tramite.getPermitidoPresentarEnRepresentacion();
    }

    /**
     * El perfil con el que actúa quien presenta de esa forma: en papel lo registra el TRAMITADOR y sin
     * papel lo presenta telemáticamente el CREADOR.
     *
     * <p>Es la equivalencia de la que sale todo lo demás de esta clase, y la que decide con qué perfil
     * entra el expediente en la máquina de estados.
     */
    public Profile getProfile(boolean presentadoEnPapel) {
        return presentadoEnPapel ? Profile.TRAMITADOR : Profile.CREADOR;
    }

    /**
     * Si hay que preguntar al usuario cómo está presentando. Solo hace falta cuando tiene los dos
     * perfiles: con uno solo el contexto se deduce de él y preguntar sobraría.
     */
    public boolean esNecesarioPresentadoEnPapel(Tramite tramite, User user, Centro centro) {
        Set<Profile> perfiles = getPerfiles(tramite, user, centro);

        return perfiles.contains(Profile.CREADOR) && perfiles.contains(Profile.TRAMITADOR);
    }

    /**
     * El valor que se deduce cuando no hay que preguntar: el tramitador solo registra en papel y el
     * creador solo presenta telemáticamente.
     */
    public boolean deducirPresentadoEnPapel(Tramite tramite, User user, Centro centro) {
        return getPerfiles(tramite, user, centro).contains(Profile.TRAMITADOR);
    }

    public boolean puedeCrear(Tramite tramite, User user, Centro centro, boolean presentadoEnPapel, boolean presentadoEnRepresentacion) {
        if (getPerfiles(tramite, user, centro).contains(getProfile(presentadoEnPapel))==false) {
            return false;
        }

        if (permitidoPresentarEnRepresentacion(tramite)==false && presentadoEnRepresentacion==true) {
            return false;
        }
        return true;
    }

    /**
     * Si el usuario da de alta expedientes del trámite en ese centro, sin mirar cómo se presentan: le
     * vale cualquiera de los dos perfiles, porque cada uno habilita su forma de presentar.
     */
    private boolean tienePerfilParaCrear(Tramite tramite, User user, Centro centro) {
        Set<Profile> perfiles = getPerfiles(tramite, user, centro);

        return perfiles.contains(Profile.CREADOR) || perfiles.contains(Profile.TRAMITADOR);
    }

    private Set<Profile> getPerfiles(Tramite tramite, User user, Centro centro) {
        return perfilesUsuarioService.getPerfilesSobreTramite(tramite, user, centro);
    }

}
