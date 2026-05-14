package com.educaflow.subsystem.importacion.service;

import com.axelor.db.modelservice.ModelService;
import com.axelor.db.modelservice.BusinessMessages;

import java.util.Optional;

import com.educaflow.subsystem.importacion.db.TareaImportacion;

public interface TareaImportacionService extends ModelService<TareaImportacion> {

    Optional<BusinessMessages> validateInsert(TareaImportacion entidad);

    Optional<BusinessMessages> validateUpdate(TareaImportacion entidad, TareaImportacion entidadOriginal);

    Optional<BusinessMessages> validateRemove(TareaImportacion entidad);

}
