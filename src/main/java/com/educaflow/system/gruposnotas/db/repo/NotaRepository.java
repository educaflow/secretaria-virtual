package com.educaflow.system.gruposnotas.db.repo;

import com.educaflow.system.gruposnotas.db.Grupo;
import com.educaflow.system.gruposnotas.db.Nota;

import java.util.List;

public class NotaRepository extends AbstractNotaRepository {

    public List<Nota> findByGrupo(Grupo grupo) {
        return all().filter("self.moduloGrupo.grupo = :grupo").bind("grupo", grupo).fetch();
    }
}
