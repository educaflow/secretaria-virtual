package com.educaflow.subsystem.common.service;

import com.educaflow.subsystem.common.db.CentroUsuario;

import java.util.List;

public interface CentroService {

    List<CentroUsuario> getAdministradoresByCentro(Long id);

    List<CentroUsuario> getJefesEstudioByCentro(Long id);

}
