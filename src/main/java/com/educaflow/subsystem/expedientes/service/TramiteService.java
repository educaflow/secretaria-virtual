package com.educaflow.subsystem.expedientes.service;

import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.ModelService;
import com.educaflow.subsystem.expedientes.db.Tramite;

import java.util.List;
import java.util.Optional;

public interface TramiteService extends ModelService<Tramite> {


    List<Tramite> findConTipoExpedienteActivo();


    Optional<BusinessMessages> validateFindConTipoExpedienteActivo();
}
