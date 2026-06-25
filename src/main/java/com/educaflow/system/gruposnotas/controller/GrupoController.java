package com.educaflow.system.gruposnotas.controller;

import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.ModelServiceFactory;
import com.axelor.meta.CallMethod;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.educaflow.base.infrastructure.axelorhelper.ActionRequestHelper;
import com.educaflow.base.infrastructure.axelorhelper.ActionResponseHelper;
import com.educaflow.system.gruposnotas.db.Grupo;
import com.educaflow.system.gruposnotas.service.GrupoService;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;

import java.util.Optional;

public class GrupoController {

    @Inject
    private ModelServiceFactory modelServiceFactory;

    @CallMethod
    @Transactional
    public void cerrarGrupo(ActionRequest actionRequest, ActionResponse actionResponse) {
        final GrupoService grupoService = (GrupoService) modelServiceFactory.resolve(Grupo.class);

        ActionRequestHelper<Grupo> actionRequestHelper = new ActionRequestHelper(actionRequest, Grupo.class);
        ActionResponseHelper actionResponseHelper = new ActionResponseHelper(actionResponse);

        Grupo grupoOriginal = actionRequestHelper.getOriginalModel();
        Grupo grupo = actionRequestHelper.getModel(grupoService.allowPropertiesCerrarGrupo());

        Optional<BusinessMessages> validationResult = grupoService.validateCerrarGrupo(grupo, grupoOriginal);
        if (validationResult.isPresent()) {
            actionResponseHelper.doResponseBusinessMessagesAsError(validationResult.get());
            return;
        }

        grupoService.cerrarGrupo(grupo, grupoOriginal);
        actionResponse.setReload(true);
    }

    @CallMethod
    @Transactional
    public void reabrirGrupo(ActionRequest actionRequest, ActionResponse actionResponse) {
        final GrupoService grupoService = (GrupoService) modelServiceFactory.resolve(Grupo.class);

        ActionRequestHelper<Grupo> actionRequestHelper = new ActionRequestHelper(actionRequest, Grupo.class);
        ActionResponseHelper actionResponseHelper = new ActionResponseHelper(actionResponse);

        Grupo grupoOriginal = actionRequestHelper.getOriginalModel();
        Grupo grupo = actionRequestHelper.getModel(grupoService.allowPropertiesReabrirGrupo());

        Optional<BusinessMessages> validationResult = grupoService.validateReabrirGrupo(grupo, grupoOriginal);
        if (validationResult.isPresent()) {
            actionResponseHelper.doResponseBusinessMessagesAsError(validationResult.get());
            return;
        }

        grupoService.reabrirGrupo(grupo, grupoOriginal);
        actionResponse.setReload(true);
    }
}
