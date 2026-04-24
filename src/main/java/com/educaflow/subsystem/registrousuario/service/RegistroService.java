package com.educaflow.subsystem.registrousuario.service;

import com.axelor.auth.db.User;
import com.educaflow.base.infrastructure.validation.messages.BusinessException;

public interface RegistroService {

    public User registrarUsuario(DatosBasicosUsuario datosBasicosUsuario, String token) throws BusinessException;
}
