package com.educaflow.subsystem.security.controller;

import com.axelor.db.modelservice.ModelServiceFactory;
import com.axelor.meta.CallMethod;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.security.db.AceProfileCentro;
import com.educaflow.subsystem.security.service.AceProfileCentroService;
import com.google.inject.Inject;

import java.util.List;

public class AceProfileCentroController {

    // Con la lista vacía el domain quedaría en «IN ()», que no es portable; -1 no es el id de ninguna fila.
    private static final List<Long> NINGUNO = List.of(-1L);

    @Inject
    private ModelServiceFactory modelServiceFactory;

    @CallMethod
    public List<Long> idsCentrosSupervisados() {
        AceProfileCentroService aceProfileCentroService = (AceProfileCentroService) modelServiceFactory.resolve(AceProfileCentro.class);

        List<Long> ids = aceProfileCentroService.getCentrosSupervisados().stream()
                .map(Centro::getId)
                .toList();

        return ids.isEmpty() ? NINGUNO : ids;
    }

    /************************************************************************************/
    /***************************** Acciones de Validaciones *****************************/
    /************************************************************************************/

    /****************************************************************************/
    /***************************** Métodos privados *****************************/
    /****************************************************************************/

}
