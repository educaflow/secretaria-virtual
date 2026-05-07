package com.educaflow.subsystem.registrousuario.db.repo;

import com.axelor.db.JPA;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.common.db.TipoUsuario;
import com.educaflow.subsystem.registrousuario.db.UsuarioAutorizado;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class UsuarioAutorizadoRepository extends AbstractUsuarioAutorizadoRepository {



    public List<UsuarioAutorizado> findByCentroAndCurso(Long centroId, Integer curso) {
        return all()
                .filter("self.centro.id = :centroId AND self.curso = :curso")
                .bind("centroId", centroId)
                .bind("curso", curso)
                .order("tipoUsuario.name")
                .order("dni")
                .fetch();
    }

    public List<UsuarioAutorizado> findByCentroAndCursoAndDocumento(Centro centro, Integer curso, String documento) {
        return all()
                .filter("self.centro.id = :centroId AND self.curso = :curso AND self.dni = :dni")
                .bind("centroId", centro.getId())
                .bind("curso", curso)
                .bind("dni", documento)
                .fetch();
    }

    public List<UsuarioAutorizado> findByCentro(Long centroId) {
        return all()
                .filter("self.centro.id = :centroId" +
                        " AND self.tipoUsuario IS NOT NULL" +
                        " AND self.curso = (" +
                        "   SELECT MAX(ua2.curso) FROM UsuarioAutorizado ua2" +
                        "   WHERE ua2.dni = self.dni" +
                        "   AND ua2.centro.id = :centroId" +
                        "   AND ua2.tipoUsuario = self.tipoUsuario" +
                        " )")
                .bind("centroId", centroId)
                .fetch();
    }

    public List<UsuarioAutorizado> findByCentroHastaCurso(Long centroId, Integer cursoMax) {
        return all()
                .filter("self.centro.id = :centroId" +
                        " AND self.tipoUsuario IS NOT NULL" +
                        " AND self.curso = (" +
                        "   SELECT MAX(ua2.curso) FROM UsuarioAutorizado ua2" +
                        "   WHERE ua2.dni = self.dni" +
                        "   AND ua2.centro.id = :centroId" +
                        "   AND ua2.tipoUsuario = self.tipoUsuario" +
                        "   AND ua2.curso <= :cursoMax" +
                        " )")
                .bind("centroId", centroId)
                .bind("cursoMax", cursoMax)
                .fetch();
    }

    public void deleteByCentroAndCursoAndTipoUsuario(Long centroId, Integer curso, Long tipoUsuarioId) {
        JPA.em().createQuery(
                "DELETE FROM UsuarioAutorizado ua" +
                " WHERE ua.centro.id = :centroId" +
                " AND ua.curso = :curso" +
                " AND ua.tipoUsuario.id = :tipoUsuarioId")
                .setParameter("centroId", centroId)
                .setParameter("curso", curso)
                .setParameter("tipoUsuarioId", tipoUsuarioId)
                .executeUpdate();
    }

    public boolean isAuthorized(String dni) {
        return all()
                .filter("self.dni = :dni")
                .bind("dni", dni)
                .count() > 0;
    }

    /**
     * Devuelve un registro por combinación (centro, tipoUsuario), el del curso más alto.
     */
    public List<UsuarioAutorizado> findAllByDni(String dni) {
        return all()
                .filter("self.dni = :dni AND self.centro IS NOT NULL AND self.curso IS NOT NULL")
                .bind("dni", dni)
                .order("-curso")
                .fetch()
                .stream()
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(
                                r -> r.getCentro().getId() + "_" + r.getTipoUsuario().getId(),
                                r -> r,
                                (a, b) -> a  // primer elemento = curso más alto (orden desc)
                        ),
                        m -> new ArrayList<>(m.values())
                ));
    }
}
