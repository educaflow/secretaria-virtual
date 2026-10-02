package com.educaflow.subsystem.common.db.repo;

import com.educaflow.subsystem.common.db.CargoCodigo;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.common.db.CentroUsuarioCargo;

import java.util.List;

public class CentroUsuarioCargoRepository extends AbstractCentroUsuarioCargoRepository {

    public List<CentroUsuarioCargo> findByCentroAndCargo(Centro centro, CargoCodigo codigoCargo) {
        return all()
                .filter("self.centroUsuario.centro = :centro AND self.cargo.code = :codigoCargo")
                .bind("centro", centro)
                .bind("codigoCargo", codigoCargo)
                .fetch();
    }
}
