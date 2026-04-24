package com.educaflow.subsystem.importacion.adapters;

import com.axelor.data.adapter.Adapter;
import com.axelor.inject.Beans;
import com.educaflow.subsystem.security.db.repo.TipoUsuarioRepository;

import java.util.Map;

public class TipoUsuarioAdapter extends Adapter {

    private final TipoUsuarioRepository tipoUsuarioRepository;

    public TipoUsuarioAdapter() {
        this.tipoUsuarioRepository = Beans.get(TipoUsuarioRepository.class);
    }

    @Override
    public Object adapt(Object value, Map<String, Object> context) {
        String nodeName = String.valueOf(value);
        String code = "docente".equals(nodeName) ? "PROFESOR" : "ALUMNO";

        return tipoUsuarioRepository
                .all()
                .filter("self.code = :code")
                .bind("code", code)
                .fetchOne();
    }
}