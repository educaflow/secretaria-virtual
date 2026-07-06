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
 *
 * <p>Solo expone la validación remota previa a {@code save}/{@code delete}
 * ({@code validateInsert}/{@code validateUpdate}/{@code validateRemove} del
 * {@link ModelService} de la entidad), sin persistir nada: la persistencia sigue siendo
 * de las acciones predefinidas del framework. Las acciones custom de cada entidad van en
 * su propio {@code <NombreEntidad>Controller}, no aquí.</p>
 */
public class DefaultModelController {

    @Inject
    private ModelServiceFactory modelServiceFactory;

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
            Model entidad = actionRequestHelper.getModel(modelService.allowPropertiesUpdate());
            Model original = actionRequestHelper.getOriginalModel();
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

    @SuppressWarnings("unchecked")
    private Class<Model> getModelClass(ActionRequest actionRequest) {
        return (Class<Model>) new ActionRequestHelper<>(actionRequest).getModelClass();
    }
}