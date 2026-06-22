package com.educaflow.system.gruposnotas.controller;

import com.axelor.db.modelservice.ModelServiceFactory;
import com.axelor.meta.CallMethod;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.educaflow.base.infrastructure.axelorhelper.ActionRequestHelper;
import com.educaflow.system.gruposnotas.db.Nota;
import com.educaflow.system.gruposnotas.service.NotaService;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;

public class NotaController {

    @Inject
    private ModelServiceFactory modelServiceFactory;

    @CallMethod
    @Transactional
    public void guardarNota(ActionRequest actionRequest, ActionResponse actionResponse) {
        final NotaService notaService = (NotaService) modelServiceFactory.resolve(Nota.class);

        ActionRequestHelper<Nota> actionRequestHelper = new ActionRequestHelper<>(actionRequest, Nota.class);

        Nota notaOriginal = actionRequestHelper.getOriginalModel();
        Nota nota = actionRequestHelper.getModel(notaService.allowPropertiesGuardarNota());

        notaService.guardarNota(nota, notaOriginal);
    }
}
