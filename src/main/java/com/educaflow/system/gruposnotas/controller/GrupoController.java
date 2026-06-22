package com.educaflow.system.gruposnotas.controller;

import com.axelor.db.modelservice.ModelServiceFactory;
import com.axelor.meta.CallMethod;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.educaflow.base.infrastructure.axelorhelper.ActionRequestHelper;
import com.educaflow.system.gruposnotas.db.Grupo;
import com.educaflow.system.gruposnotas.service.GrupoService;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;

public class GrupoController {

    @Inject
    private ModelServiceFactory modelServiceFactory;

    @CallMethod
    @Transactional
    public void cerrar(ActionRequest actionRequest, ActionResponse actionResponse) {
        final GrupoService grupoService = (GrupoService) modelServiceFactory.resolve(Grupo.class);

        ActionRequestHelper<Grupo> actionRequestHelper = new ActionRequestHelper<>(actionRequest, Grupo.class);

        Grupo grupoOriginal = actionRequestHelper.getOriginalModel();
        Grupo grupo = actionRequestHelper.getModel(grupoService.allowPropertiesCerrar());

        grupoService.cerrar(grupo, grupoOriginal);
    }

    @CallMethod
    @Transactional
    public void reabrir(ActionRequest actionRequest, ActionResponse actionResponse) {
        final GrupoService grupoService = (GrupoService) modelServiceFactory.resolve(Grupo.class);

        ActionRequestHelper<Grupo> actionRequestHelper = new ActionRequestHelper<>(actionRequest, Grupo.class);

        Grupo grupoOriginal = actionRequestHelper.getOriginalModel();
        Grupo grupo = actionRequestHelper.getModel(grupoService.allowPropertiesReabrir());

        grupoService.reabrir(grupo, grupoOriginal);
    }
}
