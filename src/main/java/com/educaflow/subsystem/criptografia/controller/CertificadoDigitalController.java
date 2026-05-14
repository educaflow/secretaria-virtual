package com.educaflow.subsystem.criptografia.controller;

import com.axelor.db.modelservice.ModelServiceFactory;
import com.axelor.meta.CallMethod;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.educaflow.base.infrastructure.axelorhelper.ActionRequestHelper;
import com.educaflow.base.infrastructure.axelorhelper.ActionResponseHelper;
import com.axelor.db.modelservice.BusinessMessages;
import com.educaflow.base.util.AllowProperties;
import com.educaflow.subsystem.criptografia.db.CertificadoDigital;
import com.educaflow.subsystem.criptografia.service.CertificadoDigitalService;
import com.google.inject.Inject;

import java.util.Optional;

public class CertificadoDigitalController {

    @Inject
    private ModelServiceFactory modelServiceFactory;

    @CallMethod
    public void validateSave(ActionRequest actionRequest, ActionResponse actionResponse) {
        final CertificadoDigitalService service = (CertificadoDigitalService) modelServiceFactory.resolve(CertificadoDigital.class);

        ActionRequestHelper<CertificadoDigital> actionRequestHelper = new ActionRequestHelper<>(actionRequest, CertificadoDigital.class);
        ActionResponseHelper actionResponseHelper = new ActionResponseHelper(actionResponse);

        AllowProperties allowProperties = AllowProperties.createAllowAllProperties();
        CertificadoDigital certificado = actionRequestHelper.getModel(allowProperties);

        Optional<BusinessMessages> validationResult;
        if (actionRequestHelper.getId() == null) {
            validationResult = service.validateInsert(certificado);
        } else {
            CertificadoDigital certificadoOriginal = actionRequestHelper.getOriginalModel();
            validationResult = service.validateUpdate(certificado, certificadoOriginal);
        }

        if (validationResult.isPresent()) {
            actionResponseHelper.doResponseBusinessMessagesAsError(validationResult.get());
        }
    }

}
