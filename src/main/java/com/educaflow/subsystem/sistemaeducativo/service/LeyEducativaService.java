package com.educaflow.subsystem.sistemaeducativo.service;

import com.axelor.db.modelservice.ModelService;
import com.axelor.db.modelservice.BusinessMessages;
import com.educaflow.subsystem.sistemaeducativo.db.LeyEducativa;

import java.util.Optional;

public interface LeyEducativaService extends ModelService<LeyEducativa> {

    Optional<BusinessMessages> validateInsert(LeyEducativa leyEducativa);

    Optional<BusinessMessages> validateUpdate(LeyEducativa leyEducativa,LeyEducativa original);

    Optional<BusinessMessages> validateRemove(LeyEducativa leyEducativa);

}