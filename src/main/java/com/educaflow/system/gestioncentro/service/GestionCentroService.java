package com.educaflow.system.gestioncentro.service;

import java.util.List;

public interface GestionCentroService {

    /** Devuelve los cursos disponibles para ese centro (cursos que tienen todos los ficheros de importación). */
    List<String> getCursosDisponibles(Long centroId);
}