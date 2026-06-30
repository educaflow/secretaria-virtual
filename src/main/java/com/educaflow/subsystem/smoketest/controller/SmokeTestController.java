package com.educaflow.subsystem.smoketest.controller;

import com.axelor.db.modelservice.ModelServiceFactory;
import com.axelor.meta.CallMethod;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.educaflow.base.infrastructure.axelorhelper.ActionRequestHelper;
import com.educaflow.base.infrastructure.axelorhelper.ActionResponseHelper;
import com.axelor.db.modelservice.BusinessMessages;
import com.educaflow.subsystem.smoketest.db.SmokeTest;
import com.educaflow.subsystem.smoketest.service.SmokeTestService;
import com.google.inject.Inject;

import java.util.Optional;

public class SmokeTestController {


    @Inject
    private ModelServiceFactory modelServiceFactory;

    @CallMethod
    public void validateSave(ActionRequest actionRequest, ActionResponse actionResponse) {
        final SmokeTestService smokeTestService = (SmokeTestService)modelServiceFactory.resolve(SmokeTest.class);

        ActionRequestHelper<SmokeTest> actionRequestHelper = new ActionRequestHelper(actionRequest, SmokeTest.class);
        ActionResponseHelper actionResponseHelper = new ActionResponseHelper(actionResponse);

        SmokeTest smokeTestOriginal = actionRequestHelper.getOriginalModel();

        Optional<BusinessMessages> validationResult;
        if (actionRequestHelper.getId()==null) {
            SmokeTest smokeTest = actionRequestHelper.getModel(smokeTestService.allowPropertiesInsert());
            validationResult = smokeTestService.validateInsert(smokeTest);
        } else {
            SmokeTest smokeTest = actionRequestHelper.getModel(smokeTestService.allowPropertiesUpdate());
            validationResult = smokeTestService.validateUpdate(smokeTest,smokeTestOriginal);
        }
        if (validationResult.isPresent()) {
            actionResponseHelper.doResponseBusinessMessagesAsError(validationResult.get());
        }

    }

}
