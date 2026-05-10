package com.educaflow.system.gestioncentro.db.repo;

import com.axelor.db.JPA;
import com.axelor.db.JpaRepository;
import com.educaflow.subsystem.common.db.Centro;

import java.util.List;

public class GestionCentroRepository extends JpaRepository<Centro> {

    protected GestionCentroRepository() {
        super(Centro.class);
    }

    /**
     * Retorna los cursos disponibles para ese centro (cursos que tienen todos los ficheros de importación).
     */
    public List<String> getCursosDisponiblesByCentro(Long centroId) {
        List<Integer> cursos = JPA.em().createQuery(
                "SELECT ic.curso FROM GestionCentroImportacion ic " +
                        "WHERE ic.centro.id = :centroId " +
                        "AND ic.tipoUsuario.codigo IN ('PROFESOR', 'ALUMNO', 'FAMILIAR') " +
                        "GROUP BY ic.curso " +
                        "HAVING COUNT(DISTINCT ic.tipoUsuario.codigo) = 3 " +
                        "ORDER BY ic.curso ASC",
                Integer.class
        ).setParameter("centroId", centroId).getResultList();

        return cursos.stream()
                .map(String::valueOf)
                .toList();
    }
}