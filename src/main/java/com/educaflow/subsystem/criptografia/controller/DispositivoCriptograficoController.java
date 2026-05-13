package com.educaflow.subsystem.criptografia.controller;

import com.axelor.db.modelservice.ModelServiceFactory;
import com.axelor.meta.CallMethod;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.educaflow.base.infrastructure.axelorhelper.ActionRequestHelper;
import com.educaflow.base.infrastructure.axelorhelper.ActionResponseHelper;
import com.axelor.db.modelservice.BusinessMessages;
import com.educaflow.base.util.AllowProperties;
import com.educaflow.subsystem.criptografia.db.DispositivoCriptografico;
import com.educaflow.subsystem.criptografia.service.DispositivoCriptograficoService;
import com.google.inject.Inject;

import java.util.Optional;

public class DispositivoCriptograficoController {

    @Inject
    private ModelServiceFactory modelServiceFactory;

    @CallMethod
    public void validateSave(ActionRequest actionRequest, ActionResponse actionResponse) {
        final DispositivoCriptograficoService service = (DispositivoCriptograficoService) modelServiceFactory.resolve(DispositivoCriptografico.class);

        ActionRequestHelper<DispositivoCriptografico> actionRequestHelper = new ActionRequestHelper<>(actionRequest, DispositivoCriptografico.class);
        ActionResponseHelper actionResponseHelper = new ActionResponseHelper(actionResponse);

        AllowProperties allowProperties = AllowProperties.createAllowAllProperties();
        DispositivoCriptografico dispositivo = actionRequestHelper.getModel(allowProperties);

        Optional<BusinessMessages> validationResult;
        if (actionRequestHelper.getId() == null) {
            validationResult = service.validateInsert(dispositivo);
        } else {
            DispositivoCriptografico dispositivoOriginal = actionRequestHelper.getOriginalModel();
            validationResult = service.validateUpdate(dispositivo, dispositivoOriginal);
        }

        if (validationResult.isPresent()) {
            actionResponseHelper.doResponseBusinessMessagesAsError(validationResult.get());
        }
    }

}
