package com.educaflow.subsystem.sms.service;

import com.axelor.db.modelservice.AllowProperties;
import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.ModelService;
import com.educaflow.subsystem.sms.db.Sms;

import java.util.Optional;

public interface SmsService extends ModelService<Sms> {

    Sms reenviar(Sms entidad, Sms entidadOriginal);


    Optional<BusinessMessages> validateReenviar(Sms entidad, Sms entidadOriginal);


    AllowProperties allowPropertiesReenviar();
}
