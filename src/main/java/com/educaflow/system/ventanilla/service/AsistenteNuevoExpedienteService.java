package com.educaflow.system.ventanilla.service;

import com.axelor.db.modelservice.AllowProperties;
import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.ModelService;
import com.educaflow.system.ventanilla.db.AsistenteNuevoExpediente;

import java.util.Optional;

public interface AsistenteNuevoExpedienteService extends ModelService<AsistenteNuevoExpediente> {

    AsistenteNuevoExpediente prepararCentros(AsistenteNuevoExpediente asistente);
    Optional<BusinessMessages> validatePrepararCentros(AsistenteNuevoExpediente asistente);
    AllowProperties allowPropertiesPrepararCentros();

    AsistenteNuevoExpediente prepararTramites(AsistenteNuevoExpediente asistente);
    Optional<BusinessMessages> validatePrepararTramites(AsistenteNuevoExpediente asistente);
    AllowProperties allowPropertiesPrepararTramites();

    AsistenteNuevoExpediente recalcular(AsistenteNuevoExpediente asistente);
    Optional<BusinessMessages> validateRecalcular(AsistenteNuevoExpediente asistente);
    AllowProperties allowPropertiesRecalcular();

    Optional<BusinessMessages> validateTriggerInitialEvent(AsistenteNuevoExpediente asistente);
    AllowProperties allowPropertiesTriggerInitialEvent();

}
