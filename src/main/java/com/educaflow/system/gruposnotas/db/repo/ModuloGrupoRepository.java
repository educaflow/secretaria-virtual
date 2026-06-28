package com.educaflow.system.gruposnotas.db.repo;

import com.educaflow.system.gruposnotas.db.Grupo;
import com.educaflow.system.gruposnotas.db.ModuloGrupo;

import java.util.List;

public class ModuloGrupoRepository extends AbstractModuloGrupoRepository {

    /**
     * Módulos persistidos del grupo. Se consulta la BD (no la colección O2M en memoria del Grupo)
     * porque en el alta anidada de un alumno el bean Grupo gestionado no trae la colección hidratada.
     */
    public List<ModuloGrupo> findByGrupo(Grupo grupo) {
        return all().filter("self.grupo = :grupo").bind("grupo", grupo).fetch();
    }
}
