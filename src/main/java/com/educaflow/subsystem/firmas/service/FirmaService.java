package com.educaflow.subsystem.firmas.service;

import com.axelor.db.modelservice.ModelService;
import com.educaflow.base.infrastructure.validation.messages.BusinessException;
import com.educaflow.base.infrastructure.validation.messages.BusinessMessages;
import com.educaflow.subsystem.firmas.db.TareaFirma;

import java.util.Optional;

public interface FirmaService extends ModelService<TareaFirma> {

    TareaFirma insert(DatosFirma datosFirma) throws BusinessException;
    TareaFirma marcarComoFirmada(TareaFirma tareaFirma, TareaFirma tareaFirmaOriginal);
    TareaFirma marcarComoRechazada(TareaFirma tareaFirma, TareaFirma tareaFirmaOriginal);
    Optional<BusinessMessages> validarDocumentosFirmados(TareaFirma tareaFirma);
}
