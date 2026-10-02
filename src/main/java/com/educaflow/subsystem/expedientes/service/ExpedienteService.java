package com.educaflow.subsystem.expedientes.service;

import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.ModelService;
import com.educaflow.subsystem.expedientes.db.Expediente;

import java.util.Optional;

public interface ExpedienteService extends ModelService<Expediente> {

    /**
     * Añade una nota al expediente a nombre del usuario autenticado, que debe poder leerlo y no ser su
     * creador.
     */
    Expediente addNote(Long idExpediente, String mensaje);

    Optional<BusinessMessages> validateAddNote(Long idExpediente, String mensaje);
}
