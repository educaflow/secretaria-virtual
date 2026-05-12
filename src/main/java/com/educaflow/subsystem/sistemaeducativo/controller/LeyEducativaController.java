package com.educaflow.subsystem.sistemaeducativo.controller;

import com.axelor.db.JpaRepository;
import com.axelor.db.Repository;
import com.axelor.db.modelservice.ModelServiceFactory;
import com.axelor.meta.CallMethod;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.educaflow.base.infrastructure.axelorhelper.ActionRequestHelper;
import com.educaflow.base.infrastructure.axelorhelper.ActionResponseHelper;
import com.axelor.db.modelservice.BusinessMessages;
import com.educaflow.base.util.AllowProperties;
import com.educaflow.subsystem.sistemaeducativo.db.LeyEducativa;
import com.educaflow.subsystem.sistemaeducativo.service.LeyEducativaService;
import com.google.inject.Inject;

import java.util.Optional;

public class LeyEducativaController {


    @Inject
    private ModelServiceFactory modelServiceFactory;

    @CallMethod
    public void validateSave(ActionRequest actionRequest, ActionResponse actionResponse) {
        final Repository repository = JpaRepository.of(LeyEducativa.class);
        final LeyEducativaService leyEducativaService = (LeyEducativaService)modelServiceFactory.resolve(LeyEducativa.class, repository);

        ActionRequestHelper<LeyEducativa> actionRequestHelper = new ActionRequestHelper(actionRequest, LeyEducativa.class);
        ActionResponseHelper actionResponseHelper = new ActionResponseHelper(actionResponse);

        AllowProperties allowProperties = AllowProperties.createAllowAllProperties();
        LeyEducativa leyEducativa = actionRequestHelper.getModel(allowProperties);
        LeyEducativa leyEducativaOriginal = actionRequestHelper.getOriginalModel();


        Optional<BusinessMessages> validationResult;
        if (actionRequestHelper.getId()==null) {
            validationResult = leyEducativaService.validateInsert(leyEducativa);
        } else {
            validationResult = leyEducativaService.validateUpdate(leyEducativa,leyEducativaOriginal);
        }
        if (validationResult.isPresent()) {
            actionResponseHelper.doResponseBusinessMessagesAsError(validationResult.get());
        }

    }

    @CallMethod
    public void validateDelete(ActionRequest actionRequest, ActionResponse actionResponse) {
        final Repository repository = JpaRepository.of(LeyEducativa.class);
        final LeyEducativaService leyEducativaService = (LeyEducativaService)modelServiceFactory.resolve(LeyEducativa.class, repository);

        ActionRequestHelper<LeyEducativa> actionRequestHelper = new ActionRequestHelper(actionRequest, LeyEducativa.class);
        ActionResponseHelper actionResponseHelper = new ActionResponseHelper(actionResponse);

        AllowProperties allowProperties = AllowProperties.createAllowAllProperties();
        LeyEducativa leyEducativa = actionRequestHelper.getModel(allowProperties);

        Optional<BusinessMessages> validationResult =leyEducativaService.validateRemove(leyEducativa);
        if (validationResult.isPresent()) {
            actionResponseHelper.doResponseBusinessMessagesAsError(validationResult.get());
        }

    }



}