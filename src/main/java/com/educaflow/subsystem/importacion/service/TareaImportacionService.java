package com.educaflow.subsystem.importacion.service;

import com.educaflow.base.infrastructure.validation.messages.BusinessException;
import com.educaflow.base.infrastructure.validation.messages.BusinessMessages;
import com.educaflow.subsystem.importacion.db.TareaImportacion;

import java.util.Optional;

public interface TareaImportacionService {

    BusinessMessages importar(TareaImportacion tareaImportacion) throws BusinessException;
}
