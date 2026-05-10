package com.educaflow.subsystem.common.service;

import com.axelor.db.modelservice.ModelService;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.common.db.CentroUsuario;

import java.util.List;
import java.util.Optional;

public interface CentroService extends ModelService<Centro> {

    Optional<Centro> findByCodigo(String codigo);

    List<CentroUsuario> getAdministradoresByCentro(Long id);

    List<CentroUsuario> getJefesEstudioByCentro(Long id);

}
