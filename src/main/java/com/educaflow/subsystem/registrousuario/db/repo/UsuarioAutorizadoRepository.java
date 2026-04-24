package com.educaflow.subsystem.registrousuario.db.repo;

import com.educaflow.subsystem.registrousuario.db.UsuarioAutorizado;

import java.util.ArrayList;
import java.util.List;
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
