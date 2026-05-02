package com.educaflow.subsystem.common.db.repo;

import com.axelor.auth.db.User;
import com.axelor.db.JPA;
import com.axelor.db.JpaRepository;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.common.db.CentroUsuario;

public class CentroRepository extends AbstractCentroRepository {

    public Centro findByCode(String codigoCentro) {
        return JpaRepository.of(Centro.class).all()
                .filter("self.code = ?1", codigoCentro)
                .fetchOne();
    }
}
