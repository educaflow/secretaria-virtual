package com.educaflow.system.gestioncentro.service.impl;

import com.axelor.db.modelservice.BusinessMessages;
import com.educaflow.system.gestioncentro.db.repo.GestionCentroRepository;
import com.educaflow.system.gestioncentro.service.GestionCentroService;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Optional;

public class GestionCentroServiceImpl implements GestionCentroService {

    @Inject
    GestionCentroRepository gestionCentroRepository;

    @Override
    public List<String> getCursosDisponibles(Long centroId) {
        validateGetCursosDisponibles(centroId).ifPresent(BusinessMessages::throwIfInvalid);
        return gestionCentroRepository.getCursosDisponiblesByCentro(centroId);
    }


    /****************************************************************************************/
    /******************************** Métodos de Validación *********************************/
    /****************************************************************************************/

    @Override
    public Optional<BusinessMessages> validateGetCursosDisponibles(Long centroId) {
        return Optional.empty();
    }
}
