package com.educaflow.subsystem.expedientes.service;

import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.ModelService;
import com.educaflow.subsystem.expedientes.db.Tramite;

import java.util.List;
import java.util.Optional;

public interface TramiteService extends ModelService<Tramite> {

    /** Los trámites de los que se pueden crear expedientes: los que tienen un tipo de expediente activo. */
    List<Tramite> findConTipoExpedienteActivo();

    Optional<BusinessMessages> validateFindConTipoExpedienteActivo();
}
