package com.educaflow.subsystem.common.service.impl;

import com.axelor.db.Repository;
import com.axelor.db.modelservice.DefaultModelService;
import com.educaflow.subsystem.common.db.TipoUsuario;
import com.educaflow.subsystem.common.db.repo.TipoUsuarioRepository;
import com.educaflow.subsystem.common.service.TipoUsuarioService;

import java.util.List;
import java.util.Optional;

public class TipoUsuarioServiceImpl extends DefaultModelService<TipoUsuario>implements TipoUsuarioService  {


    public TipoUsuarioServiceImpl(Class<TipoUsuario> model, Repository<TipoUsuario> repository) {
        super(model, repository);
    }

    private TipoUsuarioRepository getTipoUsuarioRepository() {
        return (TipoUsuarioRepository) repository;
    }

    @Override
    public Optional<TipoUsuario> findByCodigo(String codigo) {
        return getTipoUsuarioRepository().findByCodigo(codigo);
    }

    @Override
    public List<TipoUsuario> findByDocumentoAndCurso(String documento, Integer curso) {
        TipoUsuarioRepository tipoUsuarioRepository = (TipoUsuarioRepository) repository;
        return tipoUsuarioRepository.findByDocumentoAndCurso(documento, curso);
    }
}
