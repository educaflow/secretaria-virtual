package com.educaflow.subsystem.correos.controller;

import com.axelor.db.modelservice.ModelServiceFactory;
import com.axelor.meta.CallMethod;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.educaflow.base.infrastructure.axelorhelper.ActionRequestHelper;
import com.educaflow.base.util.AllowProperties;
import com.educaflow.subsystem.correos.db.TareaCorreo;
import com.educaflow.subsystem.correos.service.TareaCorreoService;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;

import java.util.Map;

public class TareaCorreoController {

    @Inject
    private ModelServiceFactory modelServiceFactory;

    @CallMethod
    @Transactional
    public void btnReenviar(ActionRequest actionRequest, ActionResponse actionResponse) {
        final TareaCorreoService tareaCorreoService =
                (TareaCorreoService) modelServiceFactory.resolve(TareaCorreo.class);

        ActionRequestHelper<TareaCorreo> actionRequestHelper =
                new ActionRequestHelper(actionRequest, TareaCorreo.class);

        TareaCorreo tareaCorreoOriginal = actionRequestHelper.getOriginalModel();
        AllowProperties allowProperties = AllowProperties.createAllowProperties(Map.of());
        TareaCorreo tareaCorreo = actionRequestHelper.getModel(allowProperties);

        tareaCorreoService.reenviar(tareaCorreo, tareaCorreoOriginal);
    }
}
