package com.educaflow.subsystem.common.service.impl;

import com.axelor.auth.db.User;
import com.axelor.i18n.I18n;
import com.educaflow.base.infrastructure.validation.messages.BusinessException;
import com.educaflow.subsystem.common.db.CargoCodigo;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.common.db.CentroUsuarioCargo;
import com.educaflow.subsystem.common.db.repo.CentroUsuarioCargoRepository;
import com.educaflow.subsystem.common.service.DirectorCentroService;
import com.google.inject.Inject;

import java.util.List;
import java.util.Objects;

public class DirectorCentroServiceImpl implements DirectorCentroService {

    private final CentroUsuarioCargoRepository centroUsuarioCargoRepository;

    @Inject
    public DirectorCentroServiceImpl(CentroUsuarioCargoRepository centroUsuarioCargoRepository) {
        this.centroUsuarioCargoRepository = centroUsuarioCargoRepository;
    }

    @Override
    public User getDirector(Centro centro) throws BusinessException {
        Objects.requireNonNull(centro, "centro no puede ser null");

        List<CentroUsuarioCargo> directores = centroUsuarioCargoRepository.findByCentroAndCargo(centro, CargoCodigo.DIRECTOR);

        if (directores.isEmpty()) {
            throw new BusinessException(I18n.get("El centro no tiene ningún usuario con el cargo de director; avise a quien gestiona los cargos del centro"));
        }
        if (directores.size() > 1) {
            throw new BusinessException(I18n.get("El centro tiene más de un usuario con el cargo de director; avise a quien gestiona los cargos del centro"));
        }

        return directores.get(0).getCentroUsuario().getUsuario();
    }
}
