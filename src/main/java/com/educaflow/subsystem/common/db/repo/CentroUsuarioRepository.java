package com.educaflow.subsystem.common.db.repo;

import com.axelor.db.JPA;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.common.db.CentroUsuario;
import com.educaflow.subsystem.common.db.TipoUsuario;

import java.util.List;
import java.util.Optional;


public class CentroUsuarioRepository extends AbstractCentroUsuarioRepository {

    public List<CentroUsuario> getCargosByCentro(Long centroId, String tipoCode) {
        return all()
                .filter("self.centro.id = :centroId AND self.id IN " +
                        "(SELECT t.centroUsuario.id FROM CentroUsuarioTipoUsuario t " +
                        " WHERE t.centroUsuario.centro.id = :centroId AND t.tipoUsuario.codigo = :tipoCode)")
                .bind("centroId", centroId)
                .bind("tipoCode", tipoCode)
                .fetch();
    }

    public CentroUsuario findOneByCentroAndTipoUsuario(Long centroId, TipoUsuario tipoUsuario) {
        return all()
                .filter("self.centro.id = :centroId AND EXISTS " +
                        "(SELECT t FROM CentroUsuarioTipoUsuario t " +
                        " WHERE t.centroUsuario = self AND t.tipoUsuario = :tipoUsuario)")
                .bind("centroId", centroId)
                .bind("tipoUsuario", tipoUsuario)
                .fetchOne();
    }

    public CentroUsuario findOneByCentroAndTipoUsuario(Long centroId, String codigoTipo) {
        return all()
                .filter("self.centro.id = :centroId AND EXISTS " +
                        "(SELECT t FROM CentroUsuarioTipoUsuario t " +
                        " WHERE t.centroUsuario = self AND t.tipoUsuario.codigo = :codigoTipo)")
                .bind("centroId", centroId)
                .bind("codigoTipo", codigoTipo)
                .fetchOne();
    }

    public List<CentroUsuario> findByCentroAndTipoUsuario(Long centroId, TipoUsuario tipoUsuario) {
        return all()
                .filter("self.centro.id = :centroId AND EXISTS " +
                        "(SELECT t FROM CentroUsuarioTipoUsuario t " +
                        " WHERE t.centroUsuario = self AND t.tipoUsuario = :tipoUsuario)")
                .bind("centroId", centroId)
                .bind("tipoUsuario", tipoUsuario)
                .fetch();
    }

    public List<CentroUsuario> findByCentroAndTipoUsuario(Long centroId, String codigoTipo) {
        return all()
                .filter("self.centro.id = :centroId AND EXISTS " +
                        "(SELECT t FROM CentroUsuarioTipoUsuario t " +
                        " WHERE t.centroUsuario = self AND t.tipoUsuario.codigo = :codigoTipo)")
                .bind("centroId", centroId)
                .bind("codigoTipo", codigoTipo)
                .fetch();
    }

    public List<CentroUsuario> findByCentroAndCodigoTipo(Centro centro, String codigoTipo) {
        return all()
                .filter("self.centro = :centro AND EXISTS " +
                        "(SELECT t FROM CentroUsuarioTipoUsuario t " +
                        " WHERE t.centroUsuario = self AND t.tipoUsuario.codigo = :codigoTipo)")
                .bind("centro", centro)
                .bind("codigoTipo", codigoTipo)
                .fetch();
    }

    public void convertirTipo(Long centroId, Long tipoOrigenId, Long tipoDestinoId) {
        JPA.em().createQuery(
                "UPDATE CentroUsuarioTipoUsuario c" +
                " SET c.tipoUsuario = (SELECT t FROM TipoUsuario t WHERE t.id = :tipoDestinoId)" +
                " WHERE c.centroUsuario.centro.id = :centroId" +
                " AND c.tipoUsuario.id = :tipoOrigenId")
                .setParameter("centroId", centroId)
                .setParameter("tipoOrigenId", tipoOrigenId)
                .setParameter("tipoDestinoId", tipoDestinoId)
                .executeUpdate();
    }

    public void deleteTipoParaDnis(Long centroId, Long tipoId, List<String> dnis) {
        JPA.em().createQuery(
                "DELETE FROM CentroUsuarioTipoUsuario c" +
                " WHERE c.centroUsuario.centro.id = :centroId" +
                " AND c.tipoUsuario.id = :tipoId" +
                " AND c.centroUsuario.usuario.dni IN :dnis")
                .setParameter("centroId", centroId)
                .setParameter("tipoId", tipoId)
                .setParameter("dnis", dnis)
                .executeUpdate();
    }

    public List<CentroUsuario> findByCentroAndUsuarioDniIn(Long centroId, List<String> dnis) {
        return all()
                .filter("self.centro.id = :centroId AND self.usuario.dni IN :dnis")
                .bind("centroId", centroId)
                .bind("dnis", dnis)
                .fetch();
    }

    public Optional<CentroUsuario> findByCentroAndUsuarioDni(Long centroId, String dni) {
        return Optional.ofNullable(
                all()
                        .filter("self.centro.id = :centroId AND self.usuario.dni = :dni")
                        .bind("centroId", centroId)
                        .bind("dni", dni)
                        .fetchOne()
        );
    }

    public List<CentroUsuario> findByCentro(Long centroId) {
        return all()
                .filter("self.centro.id = :centroId")
                .bind("centroId", centroId)
                .fetch();
    }

    public void deleteTiposUsuario(CentroUsuario centroUsuario) {
        JPA.em().createQuery("DELETE FROM CentroUsuarioTipoUsuario c WHERE c.centroUsuario = :cu")
                .setParameter("cu", centroUsuario)
                .executeUpdate();
    }

    public void deleteTiposUsuarioByCentro(Long centroId) {
        JPA.em().createQuery("DELETE FROM CentroUsuarioTipoUsuario c WHERE c.centroUsuario.centro.id = :centroId")
                .setParameter("centroId", centroId)
                .executeUpdate();
    }

    public void deleteTiposUsuarioByCentroParaCodigos(Long centroId, List<String> codigos) {
        JPA.em().createQuery(
                        "DELETE FROM CentroUsuarioTipoUsuario c " +
                        "WHERE c.centroUsuario.centro.id = :centroId " +
                        "AND c.tipoUsuario.codigo IN :codigos")
                .setParameter("centroId", centroId)
                .setParameter("codigos", codigos)
                .executeUpdate();
    }

    public void deleteTiposUsuarioByCentroExcluyendo(Long centroId, List<String> codesExcluidos) {
        JPA.em().createQuery(
                        "DELETE FROM CentroUsuarioTipoUsuario c " +
                        "WHERE c.centroUsuario.centro.id = :centroId " +
                        "AND c.tipoUsuario.codigo NOT IN :codes")
                .setParameter("centroId", centroId)
                .setParameter("codes", codesExcluidos)
                .executeUpdate();
    }
}