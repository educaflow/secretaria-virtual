package com.educaflow.subsystem.importacion.controller;

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
import com.educaflow.subsystem.importacion.db.TareaImportacion;
import com.educaflow.subsystem.importacion.service.TareaImportacionService;
import com.google.inject.Inject;

import java.util.Map;
import java.util.Optional;

public class TareaImportacionController {

    @Inject
    private ModelServiceFactory modelServiceFactory;

    @CallMethod
    public void validateSave(ActionRequest actionRequest, ActionResponse actionResponse) {
        final TareaImportacionService tareaImportacionService = (TareaImportacionService) modelServiceFactory.resolve(TareaImportacion.class);

        ActionRequestHelper<TareaImportacion> actionRequestHelper = new ActionRequestHelper<>(actionRequest, TareaImportacion.class);
        ActionResponseHelper actionResponseHelper = new ActionResponseHelper(actionResponse);

        AllowProperties allowProperties = AllowProperties.createAllowProperties(
                Map.of("tipoFichero", Map.of(), "fichero", Map.of())
        );
        TareaImportacion tareaImportacion = actionRequestHelper.getModel(allowProperties);

        Optional<BusinessMessages> validationResult;
        if (actionRequestHelper.getId() == null) {
            validationResult = tareaImportacionService.validateInsert(tareaImportacion);
        } else {
            validationResult = tareaImportacionService.validateUpdate(tareaImportacion, null);
        }
        validationResult.ifPresent(actionResponseHelper::doResponseBusinessMessagesAsError);
    }
}