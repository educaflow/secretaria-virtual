package com.educaflow.subsystem.importacion.controller;

import com.axelor.db.modelservice.ModelServiceFactory;
import com.axelor.meta.CallMethod;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.educaflow.base.infrastructure.axelorhelper.ActionRequestHelper;
import com.educaflow.base.infrastructure.axelorhelper.ActionResponseHelper;
import com.axelor.db.modelservice.BusinessMessages;
import com.educaflow.subsystem.importacion.db.TareaImportacion;
import com.educaflow.subsystem.importacion.service.TareaImportacionService;
import com.google.inject.Inject;

import java.util.Optional;

public class TareaImportacionController {

    @Inject
    private ModelServiceFactory modelServiceFactory;

    @CallMethod
    public void validateSave(ActionRequest actionRequest, ActionResponse actionResponse) {
        var tareaImportacionService = (TareaImportacionService) modelServiceFactory.resolve(TareaImportacion.class);

        var actionRequestHelper = new ActionRequestHelper<TareaImportacion>(actionRequest, TareaImportacion.class);
        var actionResponseHelper = new ActionResponseHelper(actionResponse);

        Optional<BusinessMessages> validationResult;
        if (actionRequestHelper.getId() == null) {
            TareaImportacion tareaImportacion = actionRequestHelper.getModel(tareaImportacionService.allowPropertiesInsert());
            validationResult = tareaImportacionService.validateInsert(tareaImportacion);
        } else {
            TareaImportacion tareaImportacion = actionRequestHelper.getModel(tareaImportacionService.allowPropertiesUpdate());
            validationResult = tareaImportacionService.validateUpdate(tareaImportacion, null);
        }
        validationResult.ifPresent(actionResponseHelper::doResponseBusinessMessagesAsError);
    }
}
