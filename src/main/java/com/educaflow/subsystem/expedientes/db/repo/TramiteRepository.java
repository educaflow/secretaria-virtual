package com.educaflow.subsystem.expedientes.db.repo;

import com.educaflow.subsystem.expedientes.db.Tramite;

import java.util.List;

public class TramiteRepository extends AbstractTramiteRepository {

    /** Los trámites de los que se pueden crear expedientes: los que tienen un tipo de expediente activo. */
    public List<Tramite> findConTipoExpedienteActivo() {
        return all()
                .filter("self.defaultTipoExpediente IS NOT NULL")
                .fetch();
    }
}
