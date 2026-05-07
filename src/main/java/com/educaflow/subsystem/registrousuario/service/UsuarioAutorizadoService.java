package com.educaflow.subsystem.registrousuario.service;

import com.axelor.db.modelservice.ModelService;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.registrousuario.db.UsuarioAutorizado;

import java.util.List;

public interface UsuarioAutorizadoService extends ModelService<UsuarioAutorizado> {

    boolean isAuthorized(String dni);

    List<UsuarioAutorizado> getByCentroAndCurso(Centro centro, Integer curso);
}
