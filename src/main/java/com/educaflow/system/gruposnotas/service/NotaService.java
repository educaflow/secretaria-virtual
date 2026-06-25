package com.educaflow.system.gruposnotas.service;

import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.ModelService;
import com.educaflow.system.gruposnotas.db.Nota;

import java.util.Optional;

public interface NotaService extends ModelService<Nota> {

    Nota insert(NotaInsertDTO dto);

    Optional<BusinessMessages> validateInsert(NotaInsertDTO dto);
}
