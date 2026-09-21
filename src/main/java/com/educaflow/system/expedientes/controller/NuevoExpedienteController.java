package com.educaflow.system.expedientes.controller;

import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.ModelServiceFactory;
import com.axelor.i18n.I18n;
import com.axelor.meta.CallMethod;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.educaflow.base.infrastructure.axelorhelper.ActionRequestHelper;
import com.educaflow.base.infrastructure.axelorhelper.ActionResponseHelper;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.system.expedientes.db.NuevoExpediente;
import com.educaflow.system.expedientes.service.NuevoExpedienteService;
import com.google.inject.Inject;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class NuevoExpedienteController {

    @Inject
    private ModelServiceFactory modelServiceFactory;

    @CallMethod
    public void validatePreparar(ActionRequest actionRequest, ActionResponse actionResponse) {
        final NuevoExpedienteService nuevoExpedienteService = (NuevoExpedienteService) modelServiceFactory.resolve(NuevoExpediente.class);

        ActionRequestHelper<NuevoExpediente> actionRequestHelper = new ActionRequestHelper<>(actionRequest, NuevoExpediente.class);
        ActionResponseHelper actionResponseHelper = new ActionResponseHelper(actionResponse);

        NuevoExpediente nuevoExpediente = actionRequestHelper.getModel(nuevoExpedienteService.allowPropertiesPreparar());

        Optional<BusinessMessages> validationResult = nuevoExpedienteService.validatePreparar(nuevoExpediente);
        if (validationResult.isPresent()) {
            actionResponseHelper.doResponseBusinessMessagesAsError(validationResult.get());
        }
    }

    @CallMethod
    public void preparar(ActionRequest actionRequest, ActionResponse actionResponse) {
        final NuevoExpedienteService nuevoExpedienteService = (NuevoExpedienteService) modelServiceFactory.resolve(NuevoExpediente.class);

        ActionRequestHelper<NuevoExpediente> actionRequestHelper = new ActionRequestHelper<>(actionRequest, NuevoExpediente.class);

        NuevoExpediente nuevoExpediente = actionRequestHelper.getModel(nuevoExpedienteService.allowPropertiesPreparar());

        NuevoExpediente resultado = nuevoExpedienteService.preparar(nuevoExpediente);

        List<Map<String, Object>> centrosDisponibles = resultado.getCentrosDisponibles().stream()
                .map(this::toReference)
                .toList();

        actionResponse.setValue("nombreTramite", resultado.getNombreTramite());
        actionResponse.setValue("ayudaTramite", resultado.getAyudaTramite());
        actionResponse.setValue("centrosDisponibles", centrosDisponibles);
        actionResponse.setValue("hayUnSoloCentroDisponible", resultado.getHayUnSoloCentroDisponible());
    }

    @CallMethod
    public void recalcular(ActionRequest actionRequest, ActionResponse actionResponse) {
        final NuevoExpedienteService nuevoExpedienteService = (NuevoExpedienteService) modelServiceFactory.resolve(NuevoExpediente.class);

        ActionRequestHelper<NuevoExpediente> actionRequestHelper = new ActionRequestHelper<>(actionRequest, NuevoExpediente.class);

        NuevoExpediente nuevoExpediente = actionRequestHelper.getModel(nuevoExpedienteService.allowPropertiesRecalcular());

        NuevoExpediente resultado = nuevoExpedienteService.recalcular(nuevoExpediente);

        actionResponse.setValue("hayQuePreguntarPresentacion", resultado.getHayQuePreguntarPresentacion());
        actionResponse.setValue("presentadoEnPapelDeducido", resultado.getPresentadoEnPapelDeducido());
        actionResponse.setValue("presentadoEnPapelVigente", resultado.getPresentadoEnPapelVigente());
        actionResponse.setValue("sePuedeCrearParaMi", resultado.getSePuedeCrearParaMi());
        actionResponse.setValue("sePuedeCrearEnRepresentacion", resultado.getSePuedeCrearEnRepresentacion());
        actionResponse.setValue("hayQuePreguntarParaQuien", resultado.getHayQuePreguntarParaQuien());
    }

    @CallMethod
    public void validateCrear(ActionRequest actionRequest, ActionResponse actionResponse) {
        final NuevoExpedienteService nuevoExpedienteService = (NuevoExpedienteService) modelServiceFactory.resolve(NuevoExpediente.class);

        ActionRequestHelper<NuevoExpediente> actionRequestHelper = new ActionRequestHelper<>(actionRequest, NuevoExpediente.class);
        ActionResponseHelper actionResponseHelper = new ActionResponseHelper(actionResponse);

        NuevoExpediente nuevoExpediente = actionRequestHelper.getModel(nuevoExpedienteService.allowPropertiesCrear());

        Optional<BusinessMessages> validationResult = nuevoExpedienteService.validateCrear(nuevoExpediente);
        if (validationResult.isPresent()) {
            actionResponseHelper.doResponseBusinessMessagesAsError(I18n.get("No es posible crear el expediente"), validationResult.get());
        }
    }

    private Map<String, Object> toReference(Centro centro) {
        return Map.of("id", centro.getId(), "name", centro.getName());
    }
}
