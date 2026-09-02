package com.educaflow.subsystem.firmas.service;

import com.axelor.db.modelservice.ModelService;
import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.AllowProperties;
import com.educaflow.subsystem.firmas.db.TareaFirma;

import java.util.Optional;

public interface TareaFirmaService extends ModelService<TareaFirma> {

    TareaFirma insert(TareaFirmaInsertDTO tareaFirmaInsertDTO);
    TareaFirma marcarComoFirmada(TareaFirma tareaFirma, TareaFirma tareaFirmaOriginal);
    TareaFirma marcarComoRechazada(TareaFirma tareaFirma, TareaFirma tareaFirmaOriginal);
    Optional<BusinessMessages> validarDocumentosFirmados(TareaFirma tareaFirma);
    TareaFirma firmarEnServidor(TareaFirma tareaFirma, TareaFirma tareaFirmaOriginal);



    Optional<BusinessMessages> validateInsert(TareaFirmaInsertDTO tareaFirmaInsertDTO);
    Optional<BusinessMessages> validateMarcarComoFirmada(TareaFirma tareaFirma, TareaFirma tareaFirmaOriginal);
    Optional<BusinessMessages> validateMarcarComoRechazada(TareaFirma tareaFirma, TareaFirma tareaFirmaOriginal);
    Optional<BusinessMessages> validateValidarDocumentosFirmados(TareaFirma tareaFirma);
    Optional<BusinessMessages> validateFirmarEnServidor(TareaFirma tareaFirma, TareaFirma tareaFirmaOriginal);


    AllowProperties allowPropertiesMarcarComoFirmada();
    AllowProperties allowPropertiesMarcarComoRechazada();
    AllowProperties allowPropertiesValidarDocumentosFirmados();
    AllowProperties allowPropertiesFirmarEnServidor();

}
