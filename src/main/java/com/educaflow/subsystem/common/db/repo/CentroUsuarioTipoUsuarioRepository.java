package com.educaflow.subsystem.common.db.repo;

import com.axelor.db.JPA;
import com.axelor.db.JpaRepository;
import com.educaflow.subsystem.common.db.TipoUsuario;

import java.util.List;

public class CentroUsuarioTipoUsuarioRepository extends AbstractCentroUsuarioTipoUsuarioRepository {

    public int degradarProfesores(Long centroId) {
        return cambiarTipo(centroId, "PROFESOR", "EXPROFESOR");
    }

    public int degradarAlumnos(Long centroId) {
        return cambiarTipo(centroId, "ALUMNO", "EXALUMNO");
    }

    public int promoverAProfesor(Long centroId, List<String> dnis) {
        return cambiarTipo(centroId, "EXPROFESOR", "PROFESOR", dnis);
    }

    public int promoverAAlumno(Long centroId, List<String> dnis) {
        return cambiarTipo(centroId, "EXALUMNO", "ALUMNO", dnis);
    }

    private int cambiarTipo(Long centroId, String origen, String destino) {
        return JPA.em().createQuery(
                "UPDATE CentroUsuarioTipoUsuario cutu SET cutu.tipoUsuario = :tDestino " +
                "WHERE cutu.tipoUsuario = :tOrigen " +
                "AND cutu.centroUsuario.id IN (SELECT cu.id FROM CentroUsuario cu WHERE cu.centro.id = :centroId)")
                .setParameter("tDestino", findTipo(destino))
                .setParameter("tOrigen",  findTipo(origen))
                .setParameter("centroId", centroId)
                .executeUpdate();
    }

    private int cambiarTipo(Long centroId, String origen, String destino, List<String> dnis) {
        return JPA.em().createQuery(
                "UPDATE CentroUsuarioTipoUsuario cutu SET cutu.tipoUsuario = :tDestino " +
                "WHERE cutu.tipoUsuario = :tOrigen " +
                "AND cutu.centroUsuario.id IN " +
                "  (SELECT cu.id FROM CentroUsuario cu WHERE cu.centro.id = :centroId AND cu.usuario.dni IN :dnis)")
                .setParameter("tDestino", findTipo(destino))
                .setParameter("tOrigen",  findTipo(origen))
                .setParameter("centroId", centroId)
                .setParameter("dnis",     dnis)
                .executeUpdate();
    }

    private TipoUsuario findTipo(String code) {
        return JpaRepository.of(TipoUsuario.class).all()
                .filter("self.code = :code").bind("code", code).fetchOne();
    }
}