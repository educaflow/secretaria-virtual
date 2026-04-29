package com.educaflow.subsystem.importacion.service;

import com.axelor.db.modelservice.ModelService;
import com.educaflow.base.infrastructure.validation.messages.BusinessException;
import com.educaflow.base.infrastructure.validation.messages.BusinessMessages;
import com.educaflow.subsystem.importacion.db.TareaImportacion;

import java.util.Optional;

public interface TareaImportacionService extends ModelService<TareaImportacion> {

    //BusinessMessages importar(TareaImportacion tareaImportacion) throws BusinessException;
    @Override
    TareaImportacion insert(TareaImportacion tareaImportacion);
    @Override
    TareaImportacion update(TareaImportacion newTareaImportacion, TareaImportacion oldTareaImportacion);
    @Override
    void remove(TareaImportacion tareaImportacion);
}
