package com.educaflow.subsystem.common.service.impl;

import com.axelor.db.JpaRepository;
import com.axelor.db.Repository;
import com.axelor.db.modelservice.DefaultModelService;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.common.db.CentroUsuario;
import com.educaflow.subsystem.common.db.repo.CentroRepository;
import com.educaflow.subsystem.common.db.repo.CentroUsuarioRepository;
import com.educaflow.subsystem.common.service.CentroService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class CentroServiceImpl extends DefaultModelService<Centro> implements CentroService {

    private final Logger logger = LoggerFactory.getLogger(CentroServiceImpl.class);

    public CentroServiceImpl(Class<Centro> model, Repository repository) {
        super(model, (CentroRepository) repository);
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
        CentroUsuarioRepository centroUsuarioRepo = (CentroUsuarioRepository) JpaRepository.of(CentroUsuario.class);
        return centroUsuarioRepo.getCargosByCentro(centroId, tipoCode);
    }

}