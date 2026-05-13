package com.educaflow.subsystem.common.service.impl;

import com.axelor.db.Repository;
import com.axelor.db.modelservice.DefaultModelService;
import com.educaflow.subsystem.common.db.TipoUsuario;
import com.educaflow.subsystem.common.service.TipoUsuarioService;

public class TipoUsuarioServiceImpl extends DefaultModelService<TipoUsuario>implements TipoUsuarioService  {


    public TipoUsuarioServiceImpl(Class<TipoUsuario> model, Repository<TipoUsuario> repository) {
        super(model, repository);
    }
}
