package com.educaflow.subsystem.firmas.service.impl;

import com.axelor.db.Repository;
import com.axelor.db.modelservice.AllowProperties;
import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.DefaultModelService;
import com.axelor.i18n.I18n;
import com.educaflow.subsystem.firmas.db.DocumentoFirma;

import java.util.Optional;

public class DocumentoFirmaServiceImpl extends DefaultModelService<DocumentoFirma> {

    public DocumentoFirmaServiceImpl(Class<DocumentoFirma> model, Repository<DocumentoFirma> repository) {
        super(model, repository);
    }

    /**************************************************************************************/
    /******************************* Métodos de Validación ********************************/
    /**************************************************************************************/

    @Override
    public Optional<BusinessMessages> validateInsert(DocumentoFirma documentoFirma) {
        return Optional.of(BusinessMessages.single(I18n.get("Los documentos de firma solo los crea el servidor.")));
    }

    @Override
    public Optional<BusinessMessages> validateUpdate(DocumentoFirma nuevo, DocumentoFirma original) {
        return Optional.of(BusinessMessages.single(I18n.get("Los documentos de firma no se pueden modificar.")));
    }

    @Override
    public Optional<BusinessMessages> validateRemove(DocumentoFirma documentoFirma) {
        return Optional.of(BusinessMessages.single(I18n.get("Los documentos de firma no se pueden borrar.")));
    }

    /************************************************************************************/
    /********************************* AllowProperties **********************************/
    /************************************************************************************/

    /***********************************************************************************/
    /********************************** Action Rules ***********************************/
    /***********************************************************************************/

    /***********************************************************************************/
    /********************************* Otras funciones *********************************/
    /***********************************************************************************/

}
