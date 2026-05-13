package com.educaflow.subsystem.importacion.service;

import com.axelor.db.modelservice.ModelService;
import com.educaflow.base.infrastructure.validation.messages.BusinessMessages;
import com.educaflow.subsystem.importacion.db.TareaImportacion;

import java.util.Optional;

public interface TareaImportacionService extends ModelService<TareaImportacion> {

    Optional<BusinessMessages> validateInsert(TareaImportacion entidad);

    Optional<BusinessMessages> validateUpdate(TareaImportacion entidad, TareaImportacion entidadOriginal);

    Optional<BusinessMessages> validateRemove(TareaImportacion entidad);

}
