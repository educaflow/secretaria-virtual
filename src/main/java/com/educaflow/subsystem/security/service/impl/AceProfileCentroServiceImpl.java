package com.educaflow.subsystem.security.service.impl;

import com.axelor.auth.db.User;
import com.axelor.db.Repository;
import com.axelor.db.modelservice.AllowProperties;
import com.axelor.db.modelservice.BusinessMessage;
import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.DefaultModelService;
import com.axelor.i18n.I18n;
import com.educaflow.base.util.SecurityUtil;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.security.db.AceProfileCentro;
import com.educaflow.subsystem.security.db.repo.AceProfileCentroRepository;
import com.educaflow.subsystem.security.service.AceProfileCentroService;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public class AceProfileCentroServiceImpl extends DefaultModelService<AceProfileCentro> implements AceProfileCentroService {

    public AceProfileCentroServiceImpl(Class<AceProfileCentro> model, Repository<AceProfileCentro> repository) {
        super(model, repository);
    }

    @Override
    public List<Centro> getCentrosSupervisados() {
        validateGetCentrosSupervisados().ifPresent(BusinessMessages::throwIfInvalid);

        return ((AceProfileCentroRepository) repository).findCentrosSupervisados(SecurityUtil.getUser());
    }

    /****************************************************************************************/
    /******************************** Métodos de Validación *********************************/
    /****************************************************************************************/

    @Override
    public Optional<BusinessMessages> validateInsert(AceProfileCentro fila) {
        return validarFila(fila);
    }

    @Override
    public Optional<BusinessMessages> validateUpdate(AceProfileCentro fila, AceProfileCentro original) {
        return validarFila(fila);
    }

    @Override
    public Optional<BusinessMessages> validateRemove(AceProfileCentro fila) {
        BusinessMessages messages = new BusinessMessages();

        validarCentroGestionable(fila.getCentro(), messages);

        return messages.isValid() ? Optional.empty() : Optional.of(messages);
    }

    @Override
    public Optional<BusinessMessages> validateGetCentrosSupervisados() {
        return Optional.empty();
    }

    private Optional<BusinessMessages> validarFila(AceProfileCentro fila) {
        BusinessMessages messages = new BusinessMessages();

        validarObligatorios(fila, messages);
        validarDestinatario(fila, messages);
        if (messages.isValid() == false) {
            return Optional.of(messages);
        }

        validarCentroGestionable(fila.getCentro(), messages);
        if (fila.getUsuario() != null) {
            validarUsuarioDelCentro(fila, messages);
        }
        validarAsignacionNoRepetida(fila, messages);

        return messages.isValid() ? Optional.empty() : Optional.of(messages);
    }

    private void validarObligatorios(AceProfileCentro fila, BusinessMessages messages) {
        if (fila.getCentro() == null) {
            messages.add(new BusinessMessage("centro", I18n.get("El centro es obligatorio")));
        }
        if (fila.getTramite() == null) {
            messages.add(new BusinessMessage("tramite", I18n.get("El trámite es obligatorio")));
        }
        if (fila.getPerfil() == null) {
            messages.add(new BusinessMessage("perfil", I18n.get("El perfil es obligatorio")));
        }
    }

    private void validarCentroGestionable(Centro centro, BusinessMessages messages) {
        User user = SecurityUtil.getUser();
        boolean gestionable = SecurityUtil.isAdmin(user)
                || ((AceProfileCentroRepository) repository).findCentrosSupervisados(user).stream()
                        .anyMatch(supervisado -> Objects.equals(supervisado.getId(), centro.getId()));

        if (gestionable == false) {
            messages.add(new BusinessMessage("centro",
                    I18n.get("Solo puedes gestionar perfiles de los centros de los que eres supervisor")));
        }
    }

    private void validarDestinatario(AceProfileCentro fila, BusinessMessages messages) {
        long destinatarios = Stream.of(fila.getTipoUsuario(), fila.getCargo(), fila.getUsuario())
                .filter(Objects::nonNull)
                .count();

        if (destinatarios == 0) {
            messages.add(new BusinessMessage(
                    I18n.get("Indica a quién se da el perfil: un tipo de usuario, un cargo o un usuario")));
        } else if (destinatarios > 1) {
            messages.add(new BusinessMessage(
                    I18n.get("Indica solo uno: un tipo de usuario, un cargo o un usuario")));
        }
    }

    private void validarUsuarioDelCentro(AceProfileCentro fila, BusinessMessages messages) {
        if (fila.getUsuario().getCentroUsuario(fila.getCentro()) == null) {
            messages.add(new BusinessMessage("usuario", I18n.get("El usuario no pertenece al centro")));
        }
    }

    private void validarAsignacionNoRepetida(AceProfileCentro fila, BusinessMessages messages) {
        if (((AceProfileCentroRepository) repository).existeOtraIgual(fila)) {
            messages.add(new BusinessMessage(I18n.get("Ya existe esa asignación de perfil")));
        }
    }

    /**************************************************************************************/
    /********************************   AllowProperties   *********************************/
    /**************************************************************************************/

    @Override
    public AllowProperties allowPropertiesInsert() {
        return AllowProperties.createAllowProperties(Map.of(
                "centro", Map.of(),
                "tramite", Map.of(),
                "perfil", Map.of(),
                "tipoUsuario", Map.of(),
                "cargo", Map.of(),
                "usuario", Map.of()
        ));
    }

    @Override
    public AllowProperties allowPropertiesUpdate() {
        return AllowProperties.createAllowProperties(Map.of(
                "tramite", Map.of(),
                "perfil", Map.of(),
                "tipoUsuario", Map.of(),
                "cargo", Map.of(),
                "usuario", Map.of()
        ));
    }

    @Override
    public AllowProperties allowPropertiesRemove() {
        return AllowProperties.createDenyAllProperties();
    }

}
