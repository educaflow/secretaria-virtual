package com.educaflow.subsystem.common.service;

import com.axelor.db.modelservice.ModelService;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.common.db.CentroUsuario;

import java.util.List;

public interface CentroService extends ModelService<Centro> {

    List<CentroUsuario> getAdministradoresByCentro(Long id);

    List<CentroUsuario> getJefesEstudioByCentro(Long id);

}
