package com.educaflow.base.infrastructure.controller;

import com.axelor.db.Model;
import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.ModelService;
import com.axelor.db.modelservice.ModelServiceFactory;
import com.axelor.meta.CallMethod;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.educaflow.base.infrastructure.axelorhelper.ActionRequestHelper;
import com.educaflow.base.infrastructure.axelorhelper.ActionResponseHelper;
import com.google.inject.Inject;

import java.util.Optional;

/**
 * Controlador genérico válido para cualquier entidad: resuelve la entidad a partir del
 * {@code _model} del contexto del request, por lo que las acciones XML que lo invocan
 * (en {@code DefaultModelController.xml}) son globales y no llevan atributo {@code model}.
 */
public class DefaultModelController {

    @Inject
    private ModelServiceFactory modelServiceFactory;

    // axelor-front manda refresh-tab a la pestaña ignorando los popups, así llega al listado aunque el form se abra sobre otro popup.
    @CallMethod
    public void refreshTab(ActionRequest actionRequest, ActionResponse actionResponse) {
        actionResponse.setSignal("refresh-tab", null);
    }

    /************************************************************************************/
    /***************************** Acciones de Validaciones *****************************/
    /************************************************************************************/

    @CallMethod
    public void validateSave(ActionRequest actionRequest, ActionResponse actionResponse) {
        Class<Model> modelClass = getModelClass(actionRequest);
        final ModelService<Model> modelService = modelServiceFactory.resolve(modelClass);

        ActionRequestHelper<Model> actionRequestHelper = new ActionRequestHelper<>(actionRequest, modelClass);
        ActionResponseHelper actionResponseHelper = new ActionResponseHelper(actionResponse);

        Optional<BusinessMessages> result;
        if (actionRequestHelper.getId() == null) {
            Model entidad = actionRequestHelper.getModel(modelService.allowPropertiesInsert());
            result = modelService.validateInsert(entidad);
        } else {
            // getModel vuelca el JSON sobre la misma instancia gestionada que devuelve find(id): el original se clona antes para que no recoja esos cambios.
            Model original = actionRequestHelper.getOriginalModel();
            Model entidad = actionRequestHelper.getModel(modelService.allowPropertiesUpdate());
            result = modelService.validateUpdate(entidad, original);
        }

        if (result.isPresent()) {
            actionResponseHelper.doResponseBusinessMessagesAsError(result.get());
        }
    }

    @CallMethod
    public void validateDelete(ActionRequest actionRequest, ActionResponse actionResponse) {
        Class<Model> modelClass = getModelClass(actionRequest);
        final ModelService<Model> modelService = modelServiceFactory.resolve(modelClass);

        ActionRequestHelper<Model> actionRequestHelper = new ActionRequestHelper<>(actionRequest, modelClass);
        ActionResponseHelper actionResponseHelper = new ActionResponseHelper(actionResponse);

        Model entidad = actionRequestHelper.getModel(modelService.allowPropertiesRemove());

        Optional<BusinessMessages> result = modelService.validateRemove(entidad);

        if (result.isPresent()) {
            actionResponseHelper.doResponseBusinessMessagesAsError(result.get());
        }
    }

    /****************************************************************************/
    /***************************** Métodos privados *****************************/
    /****************************************************************************/

    @SuppressWarnings("unchecked")
    private Class<Model> getModelClass(ActionRequest actionRequest) {
        return (Class<Model>) new ActionRequestHelper<>(actionRequest).getModelClass();
    }

}