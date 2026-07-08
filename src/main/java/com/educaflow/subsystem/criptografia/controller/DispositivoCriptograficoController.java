package com.educaflow.subsystem.criptografia.controller;

import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.ModelServiceFactory;
import com.axelor.i18n.I18n;
import com.axelor.meta.CallMethod;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.educaflow.base.infrastructure.axelorhelper.ActionResponseHelper;
import com.educaflow.subsystem.criptografia.db.DispositivoCriptografico;
import com.educaflow.subsystem.criptografia.service.DispositivoCriptograficoService;
import com.google.inject.Inject;

import java.util.Optional;

public class DispositivoCriptograficoController {

    @Inject
    private ModelServiceFactory modelServiceFactory;

    @CallMethod
    public void recargarDispositivosEnEntornoCriptografico(ActionRequest actionRequest, ActionResponse actionResponse) {
        final DispositivoCriptograficoService dispositivoCriptograficoService = (DispositivoCriptograficoService) modelServiceFactory.resolve(DispositivoCriptografico.class);

        ActionResponseHelper actionResponseHelper = new ActionResponseHelper(actionResponse);

        Optional<BusinessMessages> validationResult = dispositivoCriptograficoService.validateRecargarDispositivosEnEntornoCriptografico();
        if (validationResult.isPresent()) {
            actionResponseHelper.doResponseBusinessMessagesAsError(validationResult.get());
            return;
        }

        dispositivoCriptograficoService.recargarDispositivosEnEntornoCriptografico();

        actionResponse.setNotify(I18n.get("Se han recargado los dispositivos criptográficos"));
    }

}