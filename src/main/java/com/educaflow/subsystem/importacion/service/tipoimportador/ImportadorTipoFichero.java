package com.educaflow.subsystem.importacion.service.tipoimportador;

import com.educaflow.base.infrastructure.validation.messages.BusinessException;
import com.educaflow.base.infrastructure.validation.messages.BusinessMessages;
import com.educaflow.subsystem.common.db.Centro;

public interface ImportadorTipoFichero {

    BusinessMessages importar(byte[] contenido, Centro centro, Integer curso) throws BusinessException;
}