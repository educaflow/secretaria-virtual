package com.educaflow.system.gruposnotas.db.repo;

import com.axelor.auth.db.User;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.system.gruposnotas.db.AlumnoGrupo;
import com.educaflow.system.gruposnotas.db.Grupo;

import java.util.List;

public class AlumnoGrupoRepository extends AbstractAlumnoGrupoRepository {

    public List<AlumnoGrupo> findByGrupo(Grupo grupo) {
        return all()
                .filter("self.grupo = :grupo")
                .bind("grupo", grupo)
                .fetch();
    }

    public boolean existsOtroGrupoMismoCursoAcademico(
            User alumno, Centro centro, Integer cursoAcademico, Long excludeAlumnoGrupoId) {

        StringBuilder filter = new StringBuilder(
                "self.alumno = :alumno"
                        + " AND self.grupo.centro = :centro"
                        + " AND self.grupo.cursoAcademico = :cursoAcademico");

        if (excludeAlumnoGrupoId != null) {
            filter.append(" AND self.id <> :excludeId");
        }

        var query = all()
                .filter(filter.toString())
                .bind("alumno", alumno)
                .bind("centro", centro)
                .bind("cursoAcademico", cursoAcademico);

        if (excludeAlumnoGrupoId != null) {
            query = query.bind("excludeId", excludeAlumnoGrupoId);
        }

        return query.count() > 0;
    }
}
