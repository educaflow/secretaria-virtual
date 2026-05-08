package com.educaflow.subsystem.correos.service;

import com.axelor.db.modelservice.ModelService;
import com.educaflow.base.infrastructure.validation.messages.BusinessMessages;
import com.educaflow.subsystem.correos.db.Correo;

import java.util.Optional;

public interface CorreoService extends ModelService<Correo> {

    Optional<BusinessMessages> validateInsert(Correo correo);
    Optional<BusinessMessages> validateUpdate(Correo correo, Correo correoOriginal);

    void reenviar(Correo correo);
}
