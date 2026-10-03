package com.educaflow.subsystem.common.db.repo;

import com.educaflow.subsystem.common.db.Centro;

import java.util.Optional;

public class CentroRepository extends AbstractCentroRepository {

    public Optional<Centro> findByCodigo(String codigoCentro) {
        Centro centro = all()
                .filter("self.code = :codigo")
                .bind("codigo", codigoCentro)
                .fetchOne();
        return Optional.ofNullable(centro);
    }
}
