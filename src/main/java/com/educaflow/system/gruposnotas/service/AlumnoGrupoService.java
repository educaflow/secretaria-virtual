package com.educaflow.system.gruposnotas.service;

import com.axelor.db.modelservice.ModelService;
import com.educaflow.system.gruposnotas.db.AlumnoGrupo;

public interface AlumnoGrupoService extends ModelService<AlumnoGrupo> {

    String calcularNotaMedia(AlumnoGrupo alumnoGrupo);

    AlumnoGrupo guardarAlumnoGrupo(AlumnoGrupo alumnoGrupo, Long grupoId);

}
