package com.educaflow.subsystem.common.service;

import com.axelor.db.modelservice.ModelService;
import com.educaflow.subsystem.common.db.CentroUsuario;

import java.util.List;

public interface CentroUsuarioService extends ModelService<CentroUsuario> {

    List<String> calcularTipoUsuarioRegistrado(Long centroId, Integer curso);
}