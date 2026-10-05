package com.educaflow.subsystem.criptografia.controller;

import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.ModelServiceFactory;
import com.axelor.i18n.I18n;
import com.axelor.meta.CallMethod;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.educaflow.base.infrastructure.axelorhelper.ActionRequestHelper;
import com.educaflow.base.infrastructure.axelorhelper.ActionResponseHelper;
import com.educaflow.subsystem.criptografia.db.DispositivoCriptografico;
import com.educaflow.subsystem.criptografia.service.DispositivoCriptograficoService;
import com.google.inject.Inject;

import java.util.List;
import java.util.Optional;

public class DispositivoCriptograficoController {

    private static final String CAMPO_VISTA_PKCS11_LIBRARY_PATH = "pkcs11LibraryPath";

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


    @CallMethod
    public void getSlotsDisponibles(ActionRequest actionRequest, ActionResponse actionResponse) {
        final DispositivoCriptograficoService dispositivoCriptograficoService = (DispositivoCriptograficoService) modelServiceFactory.resolve(DispositivoCriptografico.class);

        ActionRequestHelper<DispositivoCriptografico> actionRequestHelper = new ActionRequestHelper<>(actionRequest, DispositivoCriptografico.class);

        List<Integer> slotsDisponibles = dispositivoCriptograficoService.getSlotsDisponibles(getPkcs11LibraryPath(actionRequestHelper));

        actionResponse.setAttr("slot", "selection-in", slotsDisponibles);
    }

    @CallMethod
    public void getDescripcionSlotsDisponibles(ActionRequest actionRequest, ActionResponse actionResponse) {
        final DispositivoCriptograficoService dispositivoCriptograficoService = (DispositivoCriptograficoService) modelServiceFactory.resolve(DispositivoCriptografico.class);

        ActionRequestHelper<DispositivoCriptografico> actionRequestHelper = new ActionRequestHelper<>(actionRequest, DispositivoCriptografico.class);

        String descripcionSlotsDisponibles = dispositivoCriptograficoService.getDescripcionSlotsDisponibles(getPkcs11LibraryPath(actionRequestHelper));

        actionResponse.setValue("slotsDisponibles", descripcionSlotsDisponibles);
    }

    /**************************************************************************************/
    /****************************** Acciones de Validaciones ******************************/
    /**************************************************************************************/


    @CallMethod
    public void validateGetSlotsDisponibles(ActionRequest actionRequest, ActionResponse actionResponse) {
        final DispositivoCriptograficoService dispositivoCriptograficoService = (DispositivoCriptograficoService) modelServiceFactory.resolve(DispositivoCriptografico.class);

        ActionRequestHelper<DispositivoCriptografico> actionRequestHelper = new ActionRequestHelper<>(actionRequest, DispositivoCriptografico.class);
        ActionResponseHelper actionResponseHelper = new ActionResponseHelper(actionResponse);

        Optional<BusinessMessages> validationResult = dispositivoCriptograficoService.validateGetSlotsDisponibles(getPkcs11LibraryPath(actionRequestHelper));
        if (validationResult.isPresent()) {
            actionResponseHelper.doResponseBusinessMessagesAsError(validationResult.get());
        }
    }




    @CallMethod
    public void validateGetDescripcionSlotsDisponibles(ActionRequest actionRequest, ActionResponse actionResponse) {
        final DispositivoCriptograficoService dispositivoCriptograficoService = (DispositivoCriptograficoService) modelServiceFactory.resolve(DispositivoCriptografico.class);

        ActionRequestHelper<DispositivoCriptografico> actionRequestHelper = new ActionRequestHelper<>(actionRequest, DispositivoCriptografico.class);
        ActionResponseHelper actionResponseHelper = new ActionResponseHelper(actionResponse);

        Optional<BusinessMessages> validationResult = dispositivoCriptograficoService.validateGetDescripcionSlotsDisponibles(getPkcs11LibraryPath(actionRequestHelper));
        if (validationResult.isPresent()) {
            actionResponseHelper.doResponseBusinessMessagesAsError(validationResult.get());
        }
    }


    /******************************************************************************/
    /****************************** Métodos privados ******************************/
    /******************************************************************************/


    private static String getPkcs11LibraryPath(ActionRequestHelper<DispositivoCriptografico> actionRequestHelper) {
        Object pkcs11LibraryPath = actionRequestHelper.getRequestData().get(CAMPO_VISTA_PKCS11_LIBRARY_PATH);

        return pkcs11LibraryPath == null ? null : pkcs11LibraryPath.toString();
    }

}