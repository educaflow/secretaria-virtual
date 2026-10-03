package com.educaflow.subsystem.security.service.impl;

import com.axelor.auth.db.User;
import com.axelor.db.modelservice.ModelServiceFactory;
import com.axelor.inject.Beans;
import com.educaflow.base.util.SecurityUtil;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.common.db.CentroUsuario;
import com.educaflow.subsystem.expedientes.db.Expediente;
import com.educaflow.subsystem.expedientes.db.Profile;
import com.educaflow.subsystem.expedientes.db.TipoExpediente;
import com.educaflow.subsystem.expedientes.db.Tramite;
import com.educaflow.subsystem.expedientes.db.UnidadTramitadora;
import com.educaflow.subsystem.expedientes.db.UnidadTramitadoraCodigo;
import com.educaflow.subsystem.expedientes.service.TramiteService;
import com.educaflow.subsystem.security.db.repo.AceProfileCentroRepository;
import com.educaflow.subsystem.security.db.repo.AceProfileExpedienteRepository;
import com.educaflow.subsystem.security.db.repo.AceProfileGlobalRepository;
import com.educaflow.subsystem.security.db.repo.AceProfileTipoExpedienteRepository;
import com.educaflow.subsystem.security.db.repo.AceProfileTipoUsuarioTramiteRepository;
import com.educaflow.subsystem.security.db.repo.AceProfileTramiteRepository;
import com.educaflow.subsystem.security.service.PerfilesUsuarioService;
import jakarta.inject.Inject;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class PerfilesUsuarioServiceImpl implements PerfilesUsuarioService {

    @Inject
    AceProfileGlobalRepository aceProfileGlobalRepository;

    @Inject
    AceProfileTipoUsuarioTramiteRepository aceProfileTipoUsuarioTramiteRepository;

    @Inject
    AceProfileTramiteRepository aceProfileTramiteRepository;

    @Inject
    AceProfileTipoExpedienteRepository aceProfileTipoExpedienteRepository;

    @Inject
    AceProfileCentroRepository aceProfileCentroRepository;

    @Inject
    AceProfileExpedienteRepository aceProfileExpedienteRepository;

    @Inject
    ModelServiceFactory modelServiceFactory;

    @Override
    public Set<Profile> getPerfilesSobreExpediente(Expediente expediente, User user) {
        Objects.requireNonNull(expediente, "expediente no puede ser nulo");
        Objects.requireNonNull(user, "user no puede ser nulo");

        CentroUsuario centroUsuario = user.getCentroUsuario(expediente.getCentro());
        TipoExpediente tipoExpediente = expediente.getTipoExpediente();
        Set<Profile> perfiles = new LinkedHashSet<>();

        if (centroUsuario == null) {
            return Set.of();
        }

        perfiles.addAll(getPerfilesUnicamenteSobreTramite(tipoExpediente.getTramite(), centroUsuario));
        perfiles.addAll(aceProfileTipoExpedienteRepository.findPerfiles(tipoExpediente, centroUsuario));
        // IMPORTANTE: El CREADOR de estas tablas habilita a crear expedientes, no a actuar sobre los que ya existen:
        // si se conservara, cualquier alumno sería CREADOR de los expedientes de los demás alumnos.
        perfiles.remove(Profile.CREADOR);
        perfiles.addAll(aceProfileExpedienteRepository.findPerfiles(expediente, centroUsuario));
        if (isUsuarioCreadorExpediente(expediente, user)) {
            perfiles.add(Profile.CREADOR);
        }

        return Collections.unmodifiableSet(perfiles);
    }

    @Override
    public Set<Profile> getPerfilesSobreTramite(Tramite tramite, User user, Centro centro) {
        Objects.requireNonNull(tramite, "tramite no puede ser nulo");
        Objects.requireNonNull(user, "user no puede ser nulo");
        Objects.requireNonNull(centro, "centro no puede ser nulo");
        Objects.requireNonNull(tramite.getDefaultTipoExpediente(), "defaultTipoExpediente no puede ser nulo");

        CentroUsuario centroUsuario = user.getCentroUsuario(centro);
        if (centroUsuario == null) {
            return Set.of();
        }

        Set<Profile> perfiles = getPerfilesUnicamenteSobreTramite(tramite, centroUsuario);
        perfiles.addAll(aceProfileTipoExpedienteRepository.findPerfiles(tramite.getDefaultTipoExpediente(), centroUsuario));


        return Collections.unmodifiableSet(perfiles);
    }

    @Override
    public Set<Profile> getPerfilesDeInicioSobreTramite(Tramite tramite, User user, Centro centro) {
        Objects.requireNonNull(tramite, "tramite no puede ser nulo");
        Objects.requireNonNull(user, "user no puede ser nulo");
        Objects.requireNonNull(centro, "centro no puede ser nulo");

        return getPerfilesSobreTramite(tramite, user, centro).stream()
                .filter(Profile::puedeCrearExpediente)
                .collect(Collectors.toUnmodifiableSet());
    }


    /**
     * El perfil de inicio del usuario que corresponde a esa forma de presentar.
     */
    @Override
    public Profile getPerfil(Tramite tramite, User user, Centro centro, boolean presentadoEnPapel) {
        List<Profile> perfiles = getPerfilesDeInicioSobreTramite(tramite, user, centro).stream()
                .filter(profile -> profile.permitePresentacionEnPapel() == presentadoEnPapel)
                .toList();

        if (perfiles.isEmpty()) {
            throw new IllegalStateException("El usuario no tiene ningún perfil de inicio para esa forma de presentar en el centro: es una petición manipulada, la vista solo ofrece las formas que tiene");
        }

        if (perfiles.size() > 1) {
            throw new IllegalStateException("Hay más de un perfil de inicio para la misma forma de presentar: " + perfiles);
        }

        return perfiles.get(0);
    }

    @Override
    public boolean isTramitador(User user) {
        Objects.requireNonNull(user, "user no puede ser nulo");

        return tramitaAlgunTramite(user, tramite -> true);
    }

    @Override
    public boolean isTramitador(User user, UnidadTramitadoraCodigo codigoUnidadTramitadora) {
        Objects.requireNonNull(user, "user no puede ser nulo");
        Objects.requireNonNull(codigoUnidadTramitadora, "codigoUnidadTramitadora no puede ser nulo");

        return tramitaAlgunTramite(user, tramite -> codigoUnidadTramitadora == codigoUnidadTramitadora(tramite.getUnidadTramitadora()));
    }

    /******************************************************************************/
    /****************************** Métodos privados ******************************/
    /******************************************************************************/

    private Set<Profile> getPerfilesUnicamenteSobreTramite(Tramite tramite, CentroUsuario centroUsuario) {
        Objects.requireNonNull(tramite, "tramite no puede ser nulo");
        Objects.requireNonNull(tramite.getTipoUsuario(), "tipoUsuario no puede ser nulo");
        Objects.requireNonNull(centroUsuario, "centroUsuario no puede ser nulo");

        Set<Profile> perfiles = new LinkedHashSet<>();

        perfiles.addAll(aceProfileGlobalRepository.findPerfiles(centroUsuario));
        perfiles.addAll(aceProfileTipoUsuarioTramiteRepository.findPerfiles(tramite.getTipoUsuario(), centroUsuario));
        perfiles.addAll(aceProfileTramiteRepository.findPerfiles(tramite, centroUsuario));
        perfiles.addAll(aceProfileCentroRepository.findPerfiles(tramite, centroUsuario));

        return perfiles;
    }

    /** Las preguntas son «en algún centro del usuario»: quien tramita en un centro es tramitador. */
    private boolean tramitaAlgunTramite(User user, Predicate<Tramite> filtroTramite) {
        if (user.getCentroUsuarios() == null) {
            return false;
        }

        final TramiteService tramiteService = (TramiteService) modelServiceFactory.resolve(Tramite.class);
        List<Tramite> tramites = tramiteService.findConTipoExpedienteActivo().stream().filter(filtroTramite).toList();
        return user.getCentroUsuarios().stream().anyMatch(centroUsuario -> tramites.stream().anyMatch(tramite ->
                getPerfilesSobreTramite(tramite, user, centroUsuario.getCentro()).stream().anyMatch(Profile::esDeTramitacion)));
    }

    private static UnidadTramitadoraCodigo codigoUnidadTramitadora(UnidadTramitadora unidadTramitadora) {
        if (unidadTramitadora == null) {
            return null;
        }
        return unidadTramitadora.getCode();
    }

    private static boolean isUsuarioCreadorExpediente(Expediente expediente, User user) {
        Objects.requireNonNull(user, "user no puede ser nulo");
        Objects.requireNonNull(expediente, "expediente no puede ser nulo");

        User usuarioRegistrador = expediente.getUsuarioRegistrador();
        Objects.requireNonNull(usuarioRegistrador, "usuarioRegistrador no puede ser nulo");

        return  usuarioRegistrador.getId().equals(user.getId());
    }

}
