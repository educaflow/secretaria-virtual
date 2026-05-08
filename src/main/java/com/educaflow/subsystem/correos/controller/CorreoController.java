package com.educaflow.subsystem.correos.controller;

import com.axelor.db.modelservice.ModelServiceFactory;
import com.axelor.meta.CallMethod;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.educaflow.base.infrastructure.axelorhelper.ActionRequestHelper;
import com.educaflow.base.util.AllowProperties;
import com.educaflow.subsystem.correos.db.Correo;
import com.educaflow.subsystem.correos.db.EstadoCorreo;
import com.educaflow.subsystem.correos.service.CorreoService;
import jakarta.inject.Inject;

public class CorreoController {

    @Inject
    private ModelServiceFactory modelServiceFactory;

    @CallMethod
    public void reenviar(ActionRequest actionRequest, ActionResponse actionResponse) {
        final CorreoService service = (CorreoService) modelServiceFactory.resolve(Correo.class);

        ActionRequestHelper<Correo> requestHelper = new ActionRequestHelper<>(actionRequest, Correo.class);

        AllowProperties allowProperties = AllowProperties.createAllowAllProperties();
        Correo correo = requestHelper.getModel(allowProperties);

        if (correo.getEstado() != EstadoCorreo.ERROR) {
            actionResponse.setError(
                "No se puede reenviar el correo '" + correo.getAsunto()
                + "' porque su estado actual es '" + correo.getEstado()
                + "'. Solo se pueden reenviar correos en estado ERROR."
            );
            return;
        }

        service.reenviar(correo);
        actionResponse.setReload(true);
    }
}