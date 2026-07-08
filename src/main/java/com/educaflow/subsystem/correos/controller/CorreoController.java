package com.educaflow.subsystem.correos.controller;

import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.ModelServiceFactory;
import com.axelor.i18n.I18n;
import com.axelor.meta.CallMethod;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.educaflow.base.infrastructure.axelorhelper.ActionRequestHelper;
import com.educaflow.base.infrastructure.axelorhelper.ActionResponseHelper;
import com.educaflow.subsystem.correos.db.Correo;
import com.educaflow.subsystem.correos.service.CorreoService;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;

import java.util.Optional;

public class CorreoController {

    @Inject
    private ModelServiceFactory modelServiceFactory;

    @CallMethod
    public void validateReenviar(ActionRequest actionRequest, ActionResponse actionResponse) {
        final CorreoService correoService = (CorreoService) modelServiceFactory.resolve(Correo.class);

        ActionRequestHelper<Correo> actionRequestHelper = new ActionRequestHelper(actionRequest, Correo.class);
        ActionResponseHelper actionResponseHelper = new ActionResponseHelper(actionResponse);

        Correo entidadOriginal = actionRequestHelper.getOriginalModel();

        Optional<BusinessMessages> validationResult = correoService.validateReenviar(entidadOriginal, entidadOriginal);
        if (validationResult.isPresent()) {
            actionResponseHelper.doResponseBusinessMessagesAsError(validationResult.get());
        }
    }

    @CallMethod
    @Transactional
    public void reenviar(ActionRequest actionRequest, ActionResponse actionResponse) {
        final CorreoService correoService = (CorreoService) modelServiceFactory.resolve(Correo.class);

        ActionRequestHelper<Correo> actionRequestHelper = new ActionRequestHelper(actionRequest, Correo.class);

        Correo entidadOriginal = actionRequestHelper.getOriginalModel();
        Correo entidad = actionRequestHelper.getModel(correoService.allowPropertiesReenviar());

        correoService.reenviar(entidad, entidadOriginal);

        actionResponse.setNotify(I18n.get("El reenvío del correo se ha puesto en marcha."));
        actionResponse.setSignal("refresh-tab", null);
    }

}
