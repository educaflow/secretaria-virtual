package com.educaflow.subsystem.common.db.repo;

import com.educaflow.subsystem.common.db.TipoUsuario;

import java.util.List;


public class TipoUsuarioRepository extends AbstractTipoUsuarioRepository {

    public List<TipoUsuario> findByDocumentoAndCurso(String documento, Integer curso) {
        return all()
                .filter("self.documento = :documento AND self.curso = :curso")
                .bind("documento", documento)
                .bind("curso", curso)
                .fetch();
    }

    public TipoUsuario findByCodigo(String codigo) {
        return all()
                .filter("self.code = :codigo")
                .bind("codigo", codigo)
                .fetchOne();
    }
}
