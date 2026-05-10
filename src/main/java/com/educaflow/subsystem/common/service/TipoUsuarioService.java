package com.educaflow.subsystem.common.service;

import com.axelor.db.modelservice.ModelService;
import com.educaflow.subsystem.common.db.TipoUsuario;

import java.util.List;
import java.util.Optional;

public interface TipoUsuarioService extends ModelService<TipoUsuario> {

    Optional<TipoUsuario> findByCodigo(String codigo);
    List<TipoUsuario> findByDocumentoAndCurso(String documento, Integer curso);
}
