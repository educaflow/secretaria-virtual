package com.educaflow.subsystem.sistemaeducativo.service;

import com.axelor.db.modelservice.ModelService;
import com.educaflow.base.infrastructure.validation.messages.BusinessMessages;
import com.educaflow.subsystem.sistemaeducativo.db.LeyEducativa;

import java.util.Optional;

public interface LeyEducativaService extends ModelService<LeyEducativa> {

    Optional<BusinessMessages> validateInsert(LeyEducativa leyEducativa);

    Optional<BusinessMessages> validateUpdate(LeyEducativa leyEducativa);

    Optional<BusinessMessages> validateRemove(LeyEducativa leyEducativa);

}