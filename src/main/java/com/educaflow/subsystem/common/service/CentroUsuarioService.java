package com.educaflow.subsystem.common.service;

import java.util.List;

public interface CentroUsuarioService {

    List<String> calcularTipoUsuarioRegistrado(Long centroId, Integer curso);
}