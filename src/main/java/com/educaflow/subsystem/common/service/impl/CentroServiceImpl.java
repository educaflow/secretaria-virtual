package com.educaflow.subsystem.common.service.impl;

import com.axelor.db.Repository;
import com.axelor.db.modelservice.DefaultModelService;
import com.axelor.db.modelservice.ModelServiceFactory;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.common.db.CentroUsuario;
import com.educaflow.subsystem.common.service.CentroService;
import com.educaflow.subsystem.common.service.CentroUsuarioService;
import com.google.inject.Inject;

import java.util.List;

public class CentroServiceImpl extends DefaultModelService<Centro> implements CentroService {

    @Inject
    ModelServiceFactory modelServiceFactory;

    public CentroServiceImpl(Class<Centro> model, Repository<Centro> repository) {
        super(model, repository);
    }

    @Override
    public List<CentroUsuario> getAdministradoresByCentro(Long id) {
        return getCargosByCentro(id, "ADMINISTRADOR");
    }

    @Override
    public List<CentroUsuario> getJefesEstudioByCentro(Long id) {
        return getCargosByCentro(id, "JEFE_ESTUDIOS");
    }

    private List<CentroUsuario> getCargosByCentro(Long centroId, String tipoCode) {
        CentroUsuarioService centroUsuarioService = (CentroUsuarioService) modelServiceFactory.resolve(CentroUsuario.class);
        return centroUsuarioService.getCargosByCentro(centroId, tipoCode);
    }

}
