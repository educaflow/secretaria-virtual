package com.educaflow.subsystem.common.db.repo;

import com.axelor.db.JPA;
import com.educaflow.subsystem.common.db.CentroUsuario;

import java.util.List;


public class CentroUsuarioRepository extends AbstractCentroUsuarioRepository {

    public List<CentroUsuario> getCargosByCentro(Long centroId, String tipoCode) {
        return all()
                .filter("self.centro.id = :centroId AND self.id IN " +
                        "(SELECT t.centroUsuario.id FROM CentroUsuarioTipoUsuario t " +
                        " WHERE t.centroUsuario.centro.id = :centroId AND t.tipoUsuario.code = :tipoCode)")
                .bind("centroId", centroId)
                .bind("tipoCode", tipoCode)
                .fetch();
    }

    public List<CentroUsuario> findByCentro(Long centroId) {
        return all()
                .filter("self.centro.id = :centroId")
                .bind("centroId", centroId)
                .fetch();
    }

    public void deleteTiposUsuarioByCentroExcluyendo(Long centroId, List<String> codesExcluidos) {
        JPA.em().createQuery(
                        "DELETE FROM CentroUsuarioTipoUsuario c " +
                        "WHERE c.centroUsuario.centro.id = :centroId " +
                        "AND c.tipoUsuario.code NOT IN :codes")
                .setParameter("centroId", centroId)
                .setParameter("codes", codesExcluidos)
                .executeUpdate();
    }
}