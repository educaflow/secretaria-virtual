package com.educaflow.subsystem.common.service;

import com.axelor.db.modelservice.ModelService;
import com.educaflow.subsystem.common.db.CentroUsuario;
import com.educaflow.subsystem.common.db.TipoUsuario;

import java.util.Optional;

public interface CentroUsuarioService extends ModelService<CentroUsuario> {

    void calcularTiposUsuarioRegistrados(Long centroId, TipoUsuario tipoUsuario, boolean esActual);

    void agregarTipo(CentroUsuario cu, TipoUsuario tipo);

    Optional<CentroUsuario> findByCentroAndUsuarioDni(Long centroId, String dni);
}