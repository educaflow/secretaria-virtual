package com.educaflow.subsystem.registrousuario.db.repo;

import com.axelor.db.JPA;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.common.db.TipoUsuario;
import com.educaflow.subsystem.registrousuario.db.UsuarioAutorizado;

import java.util.List;
import java.util.Optional;

public class UsuarioAutorizadoRepository extends AbstractUsuarioAutorizadoRepository {

    public List<UsuarioAutorizado> findByCentro(Long centroId) {
        return all()
                .filter("self.centro.id = :centroId AND self.tipoUsuario IS NOT NULL")
                .bind("centroId", centroId)
                .order("tipoUsuario.nombre")
                .order("dni")
                .fetch();
    }

    public List<UsuarioAutorizado> findByCentroAndDocumento(Centro centro, String documento) {
        return all()
                .filter("self.centro.id = :centroId AND self.dni = :dni")
                .bind("centroId", centro.getId())
                .bind("dni", documento)
                .fetch();
    }

    public List<UsuarioAutorizado> findByCentroAndCodigoTipoUsuario(Long centroId, String codigoTipoUsuario) {
        return all()
                .filter("self.centro.id = :centroId AND self.tipoUsuario.codigo = :codigo")
                .bind("centroId", centroId)
                .bind("codigo", codigoTipoUsuario)
                .fetch();
    }

    public List<UsuarioAutorizado> findActivosByCentroAndCodigo(Long centroId, String codigoTipoUsuario) {
        return all()
                .filter("self.centro.id = :centroId AND self.tipoUsuario.codigo = :codigo AND self.activo = true")
                .bind("centroId", centroId)
                .bind("codigo", codigoTipoUsuario)
                .fetch();
    }

    public Optional<UsuarioAutorizado> findByCentroAndDocumentoAndTipoUsuario(Centro centro, String documento, TipoUsuario tipoUsuario) {
        return Optional.ofNullable(
                all()
                        .filter("self.centro.id = :centroId AND self.dni = :dni AND self.tipoUsuario.id = :tipoUsuarioId")
                        .bind("centroId", centro.getId())
                        .bind("dni", documento)
                        .bind("tipoUsuarioId", tipoUsuario.getId())
                        .fetchOne()
        );
    }

    public List<UsuarioAutorizado> findAllByDni(String dni) {
        return all()
                .filter("self.dni = :dni AND self.centro IS NOT NULL AND self.tipoUsuario IS NOT NULL")
                .bind("dni", dni)
                .fetch();
    }

    public boolean isAuthorized(String dni) {
        return all()
                .filter("self.dni = :dni")
                .bind("dni", dni)
                .count() > 0;
    }

    public void marcarTodosInactivos(Long centroId, Long tipoUsuarioId) {
        JPA.em().createQuery(
                "UPDATE UsuarioAutorizado ua SET ua.activo = false" +
                " WHERE ua.centro.id = :centroId" +
                " AND ua.tipoUsuario.id = :tipoUsuarioId")
                .setParameter("centroId", centroId)
                .setParameter("tipoUsuarioId", tipoUsuarioId)
                .executeUpdate();
    }

    public void deleteByCentroAndTipoUsuario(Long centroId, Long tipoUsuarioId) {
        JPA.em().createQuery(
                "DELETE FROM UsuarioAutorizado ua" +
                " WHERE ua.centro.id = :centroId" +
                " AND ua.tipoUsuario.id = :tipoUsuarioId")
                .setParameter("centroId", centroId)
                .setParameter("tipoUsuarioId", tipoUsuarioId)
                .executeUpdate();
    }
}