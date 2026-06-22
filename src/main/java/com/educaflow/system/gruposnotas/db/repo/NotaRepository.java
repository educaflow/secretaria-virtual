package com.educaflow.system.gruposnotas.db.repo;

import com.educaflow.system.gruposnotas.db.AlumnoGrupo;
import com.educaflow.system.gruposnotas.db.ModuloGrupo;
import com.educaflow.system.gruposnotas.db.ValorNota;

public class NotaRepository extends AbstractNotaRepository {

    public long countMatriculasHonorByModuloGrupo(ModuloGrupo moduloGrupo) {
        return all()
                .filter("self.moduloGrupo = :moduloGrupo AND self.valor = :valor")
                .bind("moduloGrupo", moduloGrupo)
                .bind("valor", ValorNota.MATRICULA_HONOR)
                .count();
    }

    public long countByModuloGrupoYAlumnoGrupo(ModuloGrupo moduloGrupo, AlumnoGrupo alumnoGrupo) {
        return all()
                .filter("self.moduloGrupo = :moduloGrupo AND self.alumnoGrupo = :alumnoGrupo")
                .bind("moduloGrupo", moduloGrupo)
                .bind("alumnoGrupo", alumnoGrupo)
                .count();
    }
}
