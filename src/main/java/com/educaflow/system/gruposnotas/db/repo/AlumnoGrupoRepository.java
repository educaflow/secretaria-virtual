package com.educaflow.system.gruposnotas.db.repo;

import com.educaflow.system.gruposnotas.db.AlumnoGrupo;
import com.educaflow.system.gruposnotas.db.Grupo;

import java.util.List;
import java.util.Map;

public class AlumnoGrupoRepository extends AbstractAlumnoGrupoRepository {

    @Override
    public Map<String, Object> populate(Map<String, Object> json, Map<String, Object> context) {
        json = super.populate(json, context);
        Object id = json != null ? json.get("id") : null;
        if (id != null) {
            AlumnoGrupo alumnoGrupo = find(Long.valueOf(id.toString()));
            if (alumnoGrupo != null) {
                json.put("notaMedia", alumnoGrupo.getNotaMedia());
            }
        }
        return json;
    }

    public List<AlumnoGrupo> findByGrupo(Grupo grupo) {
        return all().filter("self.grupo = :grupo").bind("grupo", grupo).fetch();
    }
}
