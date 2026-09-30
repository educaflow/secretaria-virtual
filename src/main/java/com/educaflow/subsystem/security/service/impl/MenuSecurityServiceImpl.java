package com.educaflow.subsystem.security.service.impl;

import com.axelor.auth.db.User;
import com.educaflow.base.util.SecurityUtil;
import com.educaflow.subsystem.common.db.TipoUsuarioCodigo;
import com.educaflow.subsystem.expedientes.db.UnidadTramitadoraCodigo;
import com.educaflow.subsystem.security.service.MenuSecurityService;
import com.educaflow.subsystem.security.service.PerfilesUsuarioService;
import jakarta.inject.Inject;

/**
 * Las preguntas sobre el usuario son siempre «en algún centro del usuario»: quien es secretario en
 * un centro y profesor en otro es de la unidad de Secretaría.
 */
public class MenuSecurityServiceImpl implements MenuSecurityService {

    @Inject
    PerfilesUsuarioService perfilesUsuarioService;

    @Override
    public boolean isVisible(String menuName) {
        User user = SecurityUtil.getUser();
        if (user == null) {
            return false;
        }
        boolean admin = SecurityUtil.isAdmin(user);
        boolean supervisor = user.tieneTipoUsuario(TipoUsuarioCodigo.SUPERVISOR);

        return switch (menuName) {
            case "firmas-delCentro-menuitem", "miCentro-menuitem" -> supervisor;
            case "tramitacion-menuitem" -> admin || perfilesUsuarioService.isTramitador(user);
            case "tramitacion-jefaturaDeEstudios-menuitem" -> perfilesUsuarioService.isTramitador(user, UnidadTramitadoraCodigo.JEFATURA_ESTUDIOS);
            case "tramitacion-secretaria-menuitem" -> perfilesUsuarioService.isTramitador(user, UnidadTramitadoraCodigo.SECRETARIA);
            case "registro-menuitem" -> admin || perfilesUsuarioService.isTramitador(user, UnidadTramitadoraCodigo.SECRETARIA) || supervisor;
            case "correos-delCentro-menuitem" -> supervisor || user.tieneTipoUsuario(TipoUsuarioCodigo.ADMINISTRATIVO);
            //El resto de menús solo dependen de sus `groups`.
            default -> true;
        };
    }
}
