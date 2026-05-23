package com.educaflow.system.gestioncentro.service;

import com.axelor.db.modelservice.BusinessMessages;

import java.util.List;
import java.util.Optional;

public interface GestionCentroService {

    /** Devuelve los cursos disponibles para ese centro (cursos que tienen todos los ficheros de importación). */
    List<String> getCursosDisponibles(Long centroId);

    Optional<BusinessMessages> validateGetCursosDisponibles(Long centroId);
}
