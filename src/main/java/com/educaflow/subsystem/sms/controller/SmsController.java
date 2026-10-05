package com.educaflow.subsystem.sms.controller;

import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.ModelServiceFactory;
import com.axelor.i18n.I18n;
import com.axelor.meta.CallMethod;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.educaflow.base.infrastructure.axelorhelper.ActionRequestHelper;
import com.educaflow.base.infrastructure.axelorhelper.ActionResponseHelper;
import com.educaflow.subsystem.sms.db.Sms;
import com.educaflow.subsystem.sms.service.SmsService;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;

import java.util.Optional;

public class SmsController {

    @Inject
    private ModelServiceFactory modelServiceFactory;

    @CallMethod
    @Transactional
    public void reenviar(ActionRequest actionRequest, ActionResponse actionResponse) {
        final SmsService smsService = (SmsService) modelServiceFactory.resolve(Sms.class);

        ActionRequestHelper<Sms> actionRequestHelper = new ActionRequestHelper<>(actionRequest, Sms.class);

        Sms entidadOriginal = actionRequestHelper.getOriginalModel();
        Sms entidad = actionRequestHelper.getModel(smsService.allowPropertiesReenviar());

        smsService.reenviar(entidad, entidadOriginal);

        actionResponse.setNotify(I18n.get("El reenvío del SMS se ha puesto en marcha."));
        actionResponse.setSignal("refresh-tab", null);
    }

    /************************************************************************************/
    /***************************** Acciones de Validaciones *****************************/
    /************************************************************************************/

    @CallMethod
    public void validateReenviar(ActionRequest actionRequest, ActionResponse actionResponse) {
        final SmsService smsService = (SmsService) modelServiceFactory.resolve(Sms.class);

        ActionRequestHelper<Sms> actionRequestHelper = new ActionRequestHelper<>(actionRequest, Sms.class);
        ActionResponseHelper actionResponseHelper = new ActionResponseHelper(actionResponse);

        Sms entidadOriginal = actionRequestHelper.getOriginalModel();
        Sms entidad = actionRequestHelper.getModel(smsService.allowPropertiesReenviar());

        Optional<BusinessMessages> validationResult = smsService.validateReenviar(entidad, entidadOriginal);
        if (validationResult.isPresent()) {
            actionResponseHelper.doResponseBusinessMessagesAsError(validationResult.get());
        }
    }

    /****************************************************************************/
    /***************************** Métodos privados *****************************/
    /****************************************************************************/

}
