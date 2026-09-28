package com.educaflow.subsystem.security.service.impl;

import com.axelor.auth.db.User;
import com.educaflow.base.util.SecurityUtil;
import com.educaflow.subsystem.common.db.CentroUsuario;
import com.educaflow.subsystem.expedientes.db.Profile;
import com.educaflow.subsystem.expedientes.db.TipoTramite;
import com.educaflow.subsystem.expedientes.db.Tramite;
import com.educaflow.subsystem.expedientes.db.repo.TramiteRepository;
import com.educaflow.subsystem.security.service.MenuSecurityService;
import com.educaflow.subsystem.security.service.PerfilesUsuarioService;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Las preguntas sobre el usuario son siempre «en algún centro del usuario»: quien es secretario en
 * un centro y profesor en otro es de la unidad de Secretaría. Nunca se usa {@code User.centroActivo}.
 */
public class MenuSecurityServiceImpl implements MenuSecurityService {

    private static final String UNIDAD_JEFATURA_ESTUDIOS = "JEFATURA_ESTUDIOS";
    private static final String UNIDAD_SECRETARIA = "SECRETARIA";
    private static final String TIPO_USUARIO_SUPERVISOR = "SUPERVISOR";
    private static final String TIPO_USUARIO_ADMINISTRATIVO = "ADMINISTRATIVO";

    @Inject
    PerfilesUsuarioService perfilesUsuarioService;

    @Inject
    TramiteRepository tramiteRepository;

    @Override
    public boolean isVisible(String menuName) {
        User user = SecurityUtil.getUser();
        if (user == null) {
            return false;
        }
        boolean admin = SecurityUtil.isAdmin(user);

        return switch (menuName) {
            case "firmas-delCentro-menuitem", "miCentro-menuitem" -> isSupervisor(user);
            case "tramitacion-menuitem" -> admin || isTramitador(user);
            case "tramitacion-jefaturaDeEstudios-menuitem" -> isDeUnidadTramitadora(user, UNIDAD_JEFATURA_ESTUDIOS);
            case "tramitacion-secretaria-menuitem" -> isDeUnidadTramitadora(user, UNIDAD_SECRETARIA);
            case "registro-menuitem" -> admin || isDeUnidadTramitadora(user, UNIDAD_SECRETARIA) || isSupervisor(user);
            case "correos-delCentro-menuitem" -> isSupervisor(user) || isAdministrativo(user);
            //El resto de menús solo dependen de sus `groups`.
            default -> true;
        };
    }

    /** Tiene algún perfil distinto de {@code CREADOR} sobre algún trámite en alguno de sus centros. */
    private boolean isTramitador(User user) {
        return tramitaAlgunTramite(user, tramite -> true);
    }

    /** Como {@link #isTramitador}, pero solo en los trámites de esa unidad tramitadora. */
    private boolean isDeUnidadTramitadora(User user, String codigoUnidadTramitadora) {
        return tramitaAlgunTramite(user, tramite -> Objects.equals(codigoUnidadTramitadora, codigoUnidadTramitadora(tramite.getTipoTramite())));
    }

    private boolean isSupervisor(User user) {
        return tieneTipoUsuario(user, TIPO_USUARIO_SUPERVISOR);
    }

    private boolean isAdministrativo(User user) {
        return tieneTipoUsuario(user, TIPO_USUARIO_ADMINISTRATIVO);
    }

    private boolean tramitaAlgunTramite(User user, Predicate<Tramite> filtroTramite) {
        List<Tramite> tramites = findTramitesEvaluables().stream().filter(filtroTramite).toList();
        for (CentroUsuario centroUsuario : centroUsuarios(user)) {
            for (Tramite tramite : tramites) {
                Set<Profile> perfiles = perfilesUsuarioService.getPerfilesSobreTramite(tramite, user, centroUsuario.getCentro());
                if (tieneAlgunoDistintoDeCreador(perfiles)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean tieneTipoUsuario(User user, String codigoTipoUsuario) {
        return centroUsuarios(user).stream()
                .filter(centroUsuario -> centroUsuario.getCentroUsuarioTipoUsuario() != null)
                .flatMap(centroUsuario -> centroUsuario.getCentroUsuarioTipoUsuario().stream())
                .anyMatch(cut -> codigoTipoUsuario.equals(cut.getTipoUsuario().getCodigo()));
    }

    private List<Tramite> findTramitesEvaluables() {
        return tramiteRepository.all()
                .filter("self.tipoTramite IS NOT NULL AND self.defaultTipoExpediente IS NOT NULL")
                .fetch();
    }

    private static List<CentroUsuario> centroUsuarios(User user) {
        return user.getCentroUsuarios() == null ? List.of() : user.getCentroUsuarios();
    }

    private static boolean tieneAlgunoDistintoDeCreador(Set<Profile> perfiles) {
        return perfiles.stream().anyMatch(perfil -> perfil != Profile.CREADOR);
    }

    private static String codigoUnidadTramitadora(TipoTramite tipoTramite) {
        if (tipoTramite == null || tipoTramite.getUnidadTramitadora() == null) {
            return null;
        }
        return tipoTramite.getUnidadTramitadora().getCode();
    }
}
