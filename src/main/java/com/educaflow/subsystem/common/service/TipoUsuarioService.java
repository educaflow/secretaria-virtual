package com.educaflow.subsystem.common.service;

import com.axelor.db.modelservice.ModelService;
import com.educaflow.subsystem.common.db.TipoUsuario;

import java.util.List;

public interface TipoUsuarioService extends ModelService<TipoUsuario> {

    List<TipoUsuario> findByDocumentoAndCurso(String documento, Integer curso);
}
