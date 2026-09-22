package com.educaflow.system.ventanilla.db.repo;

import com.axelor.db.JpaRepository;
import com.educaflow.subsystem.expedientes.db.Tramite;

import java.util.List;

public class VentanillaRepository extends JpaRepository<Tramite> {

    protected VentanillaRepository() {
        super(Tramite.class);
    }

    public List<Tramite> findTramitesEvaluables() {
        return all()
                .filter("self.tipoTramite IS NOT NULL AND self.defaultTipoExpediente IS NOT NULL")
                .fetch();
    }
}
