package com.educaflow.subsystem.registrousuario.service;

import com.educaflow.base.infrastructure.validation.messages.BusinessException;
import com.educaflow.subsystem.registrousuario.db.RegistroPendiente;


public interface RegistroPendienteService {

    RegistroPendiente insertar(RegistroPendiente registroPendiente) throws BusinessException;

    void validarCodigo(String codigo, String token) throws BusinessException;

}
