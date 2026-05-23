package com.educaflow.subsystem.registrousuario.service;

import com.axelor.auth.db.User;
import com.educaflow.base.infrastructure.validation.messages.BusinessException;

import java.util.Optional;

public interface RegistroService {

    public User registrarUsuario(DatosBasicosUsuario datosBasicosUsuario, String token) throws BusinessException;

    /**
     * Devuelve el email del {@link User} cuyo {@code dni} coincide con el indicado,
     * si existe el usuario y tiene email no vacío. En cualquier otro caso devuelve
     * {@link Optional#empty()}.
     */
    Optional<String> findEmailByDni(String dni);
}
