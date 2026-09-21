package com.educaflow.system.expedientes.service;

import com.axelor.db.modelservice.AllowProperties;
import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.ModelService;
import com.educaflow.system.expedientes.db.NuevoExpediente;

import java.util.Optional;

public interface NuevoExpedienteService extends ModelService<NuevoExpediente> {

    NuevoExpediente preparar(NuevoExpediente nuevoExpediente);

    Optional<BusinessMessages> validatePreparar(NuevoExpediente nuevoExpediente);

    AllowProperties allowPropertiesPreparar();

    NuevoExpediente recalcular(NuevoExpediente nuevoExpediente);

    Optional<BusinessMessages> validateRecalcular(NuevoExpediente nuevoExpediente);

    AllowProperties allowPropertiesRecalcular();

    Optional<BusinessMessages> validateCrear(NuevoExpediente nuevoExpediente);

    AllowProperties allowPropertiesCrear();

}
