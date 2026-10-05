package com.educaflow.subsystem.expedientes.controller;

import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.ModelServiceFactory;
import com.axelor.meta.CallMethod;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.educaflow.base.infrastructure.axelorhelper.ActionRequestHelper;
import com.educaflow.base.infrastructure.axelorhelper.ActionResponseHelper;
import com.educaflow.subsystem.expedientes.db.Expediente;
import com.educaflow.subsystem.expedientes.service.ExpedienteService;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;

import java.util.Optional;

/**
 * El botón «Añadir nota» del panel de notas que llevan todas las pantallas de un expediente
 * ({@code subsysExpedientes-template-notas-panel}, en {@code tramites/shared/template-views.xml}).
 */
public class ExpedienteController {

    /** Nombre del campo de vista en el que se teclea la nota: no es un campo del modelo. */
    private static final String CAMPO_VISTA_NUEVA_NOTA = "nuevaNota";

    @Inject
    private ModelServiceFactory modelServiceFactory;

    @CallMethod
    @Transactional
    public void addNote(ActionRequest actionRequest, ActionResponse actionResponse) {
        final ExpedienteService expedienteService = (ExpedienteService) modelServiceFactory.resolve(Expediente.class);

        ActionRequestHelper<Expediente> actionRequestHelper = new ActionRequestHelper<>(actionRequest);

        Expediente expediente = expedienteService.addNote(actionRequestHelper.getId(), getNuevaNota(actionRequestHelper));

        actionResponse.setValue("notas", expediente.getNotas());
        actionResponse.setValue(CAMPO_VISTA_NUEVA_NOTA, null);
    }

    /************************************************************************************/
    /***************************** Acciones de Validaciones *****************************/
    /************************************************************************************/

    @CallMethod
    public void validateAddNote(ActionRequest actionRequest, ActionResponse actionResponse) {
        final ExpedienteService expedienteService = (ExpedienteService) modelServiceFactory.resolve(Expediente.class);

        ActionRequestHelper<Expediente> actionRequestHelper = new ActionRequestHelper<>(actionRequest);
        ActionResponseHelper actionResponseHelper = new ActionResponseHelper(actionResponse);

        Optional<BusinessMessages> validationResult = expedienteService.validateAddNote(actionRequestHelper.getId(), getNuevaNota(actionRequestHelper));
        if (validationResult.isPresent()) {
            actionResponseHelper.doResponseBusinessMessagesAsError(validationResult.get());
        }
    }

    /****************************************************************************/
    /***************************** Métodos privados *****************************/
    /****************************************************************************/

    private static String getNuevaNota(ActionRequestHelper<Expediente> actionRequestHelper) {
        return (String) actionRequestHelper.getRequestData().get(CAMPO_VISTA_NUEVA_NOTA);
    }

}
