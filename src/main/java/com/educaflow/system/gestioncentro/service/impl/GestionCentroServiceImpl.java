package com.educaflow.system.gestioncentro.service.impl;

import com.educaflow.system.gestioncentro.db.repo.GestionCentroRepository;
import com.educaflow.system.gestioncentro.service.GestionCentroService;
import jakarta.inject.Inject;

import java.util.List;

public class GestionCentroServiceImpl implements GestionCentroService {

    @Inject
    GestionCentroRepository gestionCentroRepository;

    @Override
    public List<String> getCursosDisponibles(Long centroId) {
        return gestionCentroRepository.getCursosDisponiblesByCentro(centroId);
    }
}