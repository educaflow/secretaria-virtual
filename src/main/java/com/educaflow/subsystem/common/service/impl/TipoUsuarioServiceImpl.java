package com.educaflow.subsystem.common.service.impl;

import com.axelor.db.Repository;
import com.axelor.db.modelservice.DefaultModelService;
import com.educaflow.subsystem.common.db.TipoUsuario;
import com.educaflow.subsystem.common.db.repo.TipoUsuarioRepository;
import com.educaflow.subsystem.common.service.TipoUsuarioService;

import java.util.List;

public class TipoUsuarioServiceImpl extends DefaultModelService<TipoUsuario>implements TipoUsuarioService  {


    public TipoUsuarioServiceImpl(Class<TipoUsuario> model, Repository<TipoUsuario> repository) {
        super(model, repository);
    }

    @Override
    public List<TipoUsuario> findByDocumentoAndCurso(String documento, Integer curso) {
        TipoUsuarioRepository tipoUsuarioRepository = (TipoUsuarioRepository) repository;
        return tipoUsuarioRepository.findByDocumentoAndCurso(documento, curso);
    }
}
