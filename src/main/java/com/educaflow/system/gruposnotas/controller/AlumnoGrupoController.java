package com.educaflow.system.gruposnotas.controller;

import com.axelor.db.modelservice.ModelServiceFactory;
import com.axelor.meta.CallMethod;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.educaflow.base.infrastructure.axelorhelper.ActionRequestHelper;
import com.educaflow.system.gruposnotas.db.AlumnoGrupo;
import com.educaflow.system.gruposnotas.service.AlumnoGrupoService;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;

public class AlumnoGrupoController {

    @Inject
    private ModelServiceFactory modelServiceFactory;

    @CallMethod
    @Transactional
    public void guardarAlumnoGrupo(ActionRequest actionRequest, ActionResponse actionResponse) {
        AlumnoGrupoService alumnoGrupoService =
                (AlumnoGrupoService) modelServiceFactory.resolve(AlumnoGrupo.class);

        ActionRequestHelper<AlumnoGrupo> actionRequestHelper =
                new ActionRequestHelper<>(actionRequest, AlumnoGrupo.class);

        AlumnoGrupo alumnoGrupo = actionRequestHelper.getModel(alumnoGrupoService.allowPropertiesInsert());
        Long grupoId = actionRequestHelper.getParentId();

        alumnoGrupoService.guardarAlumnoGrupo(alumnoGrupo, grupoId);

        actionResponse.setSignal("back", null);
    }
}
