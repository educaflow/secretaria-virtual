package com.educaflow.subsystem.firmas.service;

import com.educaflow.base.infrastructure.validation.messages.BusinessException;
import com.educaflow.subsystem.firmas.db.TareaFirma;

public interface FirmaService {

    TareaFirma insert(DatosFirma datosFirma) throws BusinessException;
    TareaFirma marcarComoFirmada(TareaFirma tareaFirma, TareaFirma tareaFirmaOriginal) throws BusinessException;
    TareaFirma marcarComoRechazada(TareaFirma tareaFirma, TareaFirma tareaFirmaOriginal) throws BusinessException;
}
