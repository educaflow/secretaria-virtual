package com.educaflow.subsystem.common.service.impl;

import com.axelor.db.Repository;
import com.axelor.db.modelservice.DefaultModelService;
import com.educaflow.subsystem.common.db.TipoUsuario;
import com.educaflow.subsystem.common.db.repo.AbstractTipoUsuarioRepository;
import com.educaflow.subsystem.common.service.TipoUsuarioService;

import java.util.Optional;

public class TipoUsuarioServiceImpl
        extends DefaultModelService<TipoUsuario>
        implements TipoUsuarioService {

    public TipoUsuarioServiceImpl(Class<TipoUsuario> model,
                                   Repository<TipoUsuario> repository) {
        super(model, repository);
    }

    @Override
    public Optional<TipoUsuario> findByCodigo(String codigo) {
        return Optional.ofNullable(
            ((AbstractTipoUsuarioRepository) repository).findByCodigo(codigo)
        );
    }
}
