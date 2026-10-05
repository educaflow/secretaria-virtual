package com.educaflow.subsystem.expedientes.service;

import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.ModelService;
import com.educaflow.subsystem.expedientes.db.Expediente;

import java.util.Optional;

public interface ExpedienteService extends ModelService<Expediente> {


    Expediente addNote(Long idExpediente, String mensaje);


    Optional<BusinessMessages> validateAddNote(Long idExpediente, String mensaje);
}
