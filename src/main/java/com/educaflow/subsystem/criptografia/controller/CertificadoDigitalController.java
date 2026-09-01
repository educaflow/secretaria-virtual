package com.educaflow.subsystem.criptografia.controller;

import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.ModelServiceFactory;
import com.axelor.meta.CallMethod;
import com.axelor.rpc.ActionResponse;
import com.axelor.rpc.Response;
import com.educaflow.base.infrastructure.axelorhelper.ActionResponseHelper;
import com.educaflow.subsystem.criptografia.db.CertificadoDigital;
import com.educaflow.subsystem.criptografia.service.CertificadoDigitalService;
import com.educaflow.subsystem.criptografia.service.TipoAlmacenClave;
import com.google.inject.Inject;

import java.util.Optional;

public class CertificadoDigitalController {

    @Inject
    private ModelServiceFactory modelServiceFactory;

    @CallMethod
    public Response validateGetTipoAlmacenClaveByDni(String dni) {
        final CertificadoDigitalService certificadoDigitalService = (CertificadoDigitalService) modelServiceFactory.resolve(CertificadoDigital.class);

        ActionResponse actionResponse = new ActionResponse();
        ActionResponseHelper actionResponseHelper = new ActionResponseHelper(actionResponse);

        Optional<BusinessMessages> validationResult = certificadoDigitalService.validateGetTipoAlmacenClaveByDni(dni);
        if (validationResult.isPresent()) {
            actionResponseHelper.doResponseBusinessMessagesAsError(validationResult.get());
        }

        return actionResponse;
    }

    @CallMethod
    public String getTipoAlmacenClaveByDni(String dni) {
        final CertificadoDigitalService certificadoDigitalService = (CertificadoDigitalService) modelServiceFactory.resolve(CertificadoDigital.class);

        TipoAlmacenClave tipoAlmacenClave = certificadoDigitalService.getTipoAlmacenClaveByDni(dni);

        return (tipoAlmacenClave == null) ? null : tipoAlmacenClave.name();
    }

}
