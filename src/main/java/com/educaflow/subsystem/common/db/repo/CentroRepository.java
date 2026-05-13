package com.educaflow.subsystem.common.db.repo;

import com.educaflow.subsystem.common.db.Centro;

public class CentroRepository extends AbstractCentroRepository {

    public Centro findByCode(String codigoCentro) {
        return all()
                .filter("self.code = ?1", codigoCentro)
                .fetchOne();
    }
}
