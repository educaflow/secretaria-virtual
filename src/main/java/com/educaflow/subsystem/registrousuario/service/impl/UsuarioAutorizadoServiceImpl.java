package com.educaflow.subsystem.registrousuario.service.impl;

import com.axelor.auth.db.User;
import com.axelor.db.Repository;
import com.axelor.db.modelservice.AllowProperties;
import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.DefaultModelService;
import com.axelor.i18n.I18n;
import com.educaflow.base.util.SecurityUtil;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.common.db.TipoUsuarioCodigo;
import com.educaflow.subsystem.registrousuario.db.UsuarioAutorizado;
import com.educaflow.subsystem.registrousuario.service.UsuarioAutorizadoService;

import java.util.Map;
import java.util.Optional;

public class UsuarioAutorizadoServiceImpl extends DefaultModelService<UsuarioAutorizado> implements UsuarioAutorizadoService {

    public UsuarioAutorizadoServiceImpl(Class<UsuarioAutorizado> model, Repository<UsuarioAutorizado> repository) {
        super(model, repository);
    }

    /**************************************************************************************/
    /******************************* Métodos de Validación ********************************/
    /**************************************************************************************/

    /**
     * El centro lo rellena la vista con el centro padre, pero por el endpoint REST automático llega cualquiera:
     * aquí es donde se comprueba que el usuario gestiona ese centro.
     */
    @Override
    public Optional<BusinessMessages> validateInsert(UsuarioAutorizado usuarioAutorizado) {
        if (usuarioAutorizado.getCentro() == null) {
            return Optional.of(BusinessMessages.single(I18n.get("El centro es obligatorio")));
        }

        return validateGestionaCentro(usuarioAutorizado.getCentro());
    }

    @Override
    public Optional<BusinessMessages> validateUpdate(UsuarioAutorizado usuarioAutorizado, UsuarioAutorizado usuarioAutorizadoOriginal) {
        return validateGestionaCentro(usuarioAutorizadoOriginal == null ? null : usuarioAutorizadoOriginal.getCentro());
    }

    @Override
    public Optional<BusinessMessages> validateRemove(UsuarioAutorizado usuarioAutorizado) {
        return validateGestionaCentro(usuarioAutorizado.getCentro());
    }

    private static Optional<BusinessMessages> validateGestionaCentro(Centro centro) {
        User user = SecurityUtil.getUser();

        boolean gestionaCentro = user != null
                && (SecurityUtil.isAdmin(user) || (centro != null && user.tieneTipoUsuario(centro, TipoUsuarioCodigo.SUPERVISOR)));

        if (gestionaCentro == false) {
            return Optional.of(BusinessMessages.single(I18n.get("No puede gestionar los usuarios autorizados de un centro que no supervisa")));
        }

        return Optional.empty();
    }

    /************************************************************************************/
    /********************************* AllowProperties **********************************/
    /************************************************************************************/

    /** {@code fechaExportacion} no la dicta el cliente: queda fuera de la whitelist. */
    @Override
    public AllowProperties allowPropertiesInsert() {
        return AllowProperties.createAllowProperties(Map.of(
                "centro", Map.of(),
                "curso", Map.of(),
                "dni", Map.of(),
                "tipoUsuario", Map.of()
        ));
    }

    /** {@code centro} y {@code curso} son inmutables tras el alta: una autorización no se mueve de centro ni de curso. */
    @Override
    public AllowProperties allowPropertiesUpdate() {
        return AllowProperties.createAllowProperties(Map.of(
                "dni", Map.of(),
                "tipoUsuario", Map.of()
        ));
    }

    /***********************************************************************************/
    /********************************** Action Rules ***********************************/
    /***********************************************************************************/

    /***********************************************************************************/
    /********************************* Otras funciones *********************************/
    /***********************************************************************************/

}
