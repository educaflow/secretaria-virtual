package com.educaflow.system.ventanilla.service;

import com.axelor.db.modelservice.AllowProperties;
import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.ModelService;
import com.educaflow.system.ventanilla.db.AsistenteNuevoExpediente;

import java.util.Optional;

public interface AsistenteNuevoExpedienteService extends ModelService<AsistenteNuevoExpediente> {

    AsistenteNuevoExpediente prepararCentros(AsistenteNuevoExpediente asistente);
    AsistenteNuevoExpediente prepararTramites(AsistenteNuevoExpediente asistente);
    AsistenteNuevoExpediente recalcular(AsistenteNuevoExpediente asistente);


    Optional<BusinessMessages> validatePrepararCentros(AsistenteNuevoExpediente asistente);
    Optional<BusinessMessages> validatePrepararTramites(AsistenteNuevoExpediente asistente);
    Optional<BusinessMessages> validateRecalcular(AsistenteNuevoExpediente asistente);
    Optional<BusinessMessages> validateTriggerInitialEvent(AsistenteNuevoExpediente asistente);


    AllowProperties allowPropertiesPrepararCentros();
    AllowProperties allowPropertiesPrepararTramites();
    AllowProperties allowPropertiesRecalcular();
    AllowProperties allowPropertiesTriggerInitialEvent();

}
