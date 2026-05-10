package com.educaflow.subsystem.common.db.repo;

import com.educaflow.subsystem.common.db.TipoUsuario;

import java.util.List;
import java.util.Optional;


public class TipoUsuarioRepository extends AbstractTipoUsuarioRepository {

    public List<TipoUsuario> findByDocumentoAndCurso(String documento, Integer curso) {
        return all()
                .filter("self.documento = :documento AND self.curso = :curso")
                .bind("documento", documento)
                .bind("curso", curso)
                .fetch();
    }

    public Optional<TipoUsuario> findByCodigo(String codigo) {
        TipoUsuario tipoUsuario = all()
                .filter("self.codigo = :codigo")
                .bind("codigo", codigo)
                .fetchOne();
        return Optional.ofNullable(tipoUsuario);
    }
}
