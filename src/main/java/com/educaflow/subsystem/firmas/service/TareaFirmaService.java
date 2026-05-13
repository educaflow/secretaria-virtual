package com.educaflow.subsystem.firmas.service;

import com.axelor.db.modelservice.ModelService;
import com.axelor.db.modelservice.BusinessMessages;
import com.educaflow.subsystem.firmas.db.TareaFirma;

import java.util.Optional;

public interface TareaFirmaService extends ModelService<TareaFirma> {

    TareaFirma insert(TareaFirmaInsertDTO tareaFirmaInsertDTO);
    TareaFirma marcarComoFirmada(TareaFirma tareaFirma, TareaFirma tareaFirmaOriginal);
    TareaFirma marcarComoRechazada(TareaFirma tareaFirma, TareaFirma tareaFirmaOriginal);
    Optional<BusinessMessages> validarDocumentosFirmados(TareaFirma tareaFirma);
}
