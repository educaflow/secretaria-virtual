package com.educaflow.subsystem.importacion.service;

import com.axelor.db.modelservice.ModelService;
import com.educaflow.subsystem.importacion.db.TareaImportacion;

public interface TareaImportacionService extends ModelService<TareaImportacion> {

    //BusinessMessages importar(TareaImportacion tareaImportacion) throws BusinessException;
    @Override
    TareaImportacion insert(TareaImportacion tareaImportacion);
    @Override
    TareaImportacion update(TareaImportacion newTareaImportacion, TareaImportacion oldTareaImportacion);
    @Override
    void remove(TareaImportacion tareaImportacion);
}
