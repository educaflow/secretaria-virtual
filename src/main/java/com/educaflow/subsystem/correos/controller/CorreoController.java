package com.educaflow.subsystem.correos.controller;

import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.ModelServiceFactory;
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
    @Transactional
    public void btnReenviar(ActionRequest actionRequest, ActionResponse actionResponse) {
        final CorreoService correoService = (CorreoService) modelServiceFactory.resolve(Correo.class);

        ActionRequestHelper<Correo> actionRequestHelper = new ActionRequestHelper(actionRequest, Correo.class);
        ActionResponseHelper actionResponseHelper = new ActionResponseHelper(actionResponse);

        Correo correoOriginal = actionRequestHelper.getOriginalModel();
        Correo correo = actionRequestHelper.getModel(correoService.allowPropertiesReenviar());

        Optional<BusinessMessages> validationResult = correoService.validateReenviar(correo, correoOriginal);
        if (validationResult.isPresent()) {
            actionResponseHelper.doResponseBusinessMessagesAsError(validationResult.get());
            return;
        }

        correoService.reenviar(correo, correoOriginal);

        actionResponse.setSignal("refresh-tab", null);
    }

    @CallMethod
    public void onChangeDni(ActionRequest actionRequest, ActionResponse actionResponse) {
        final CorreoService correoService = (CorreoService) modelServiceFactory.resolve(Correo.class);

        ActionRequestHelper<Correo> actionRequestHelper = new ActionRequestHelper(actionRequest, Correo.class);

        Object dniObject = actionRequestHelper.getRequestData().get("dniDestinatario");
        String dni = (dniObject == null) ? null : dniObject.toString();

        String email = correoService.proponerEmailPorDni(dni);

        actionResponse.setValue("emailDestinatario", email);
    }

}
