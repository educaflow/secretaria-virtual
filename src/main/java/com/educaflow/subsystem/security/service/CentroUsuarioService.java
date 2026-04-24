package com.educaflow.subsystem.security.service;

import java.util.List;

public interface CentroUsuarioService {

    /**
     * Actualiza los tipos de usuario registrados al cambiar de curso:
     * degrada a ex-profesor/ex-alumno a los que ya no están en el centro,
     * y promueve a profesor/alumno a los que sí aparecen en el nuevo curso.
     */
    List<String> calcularTipoUsuarioRegistrado(Long centroId, Integer curso);
}
