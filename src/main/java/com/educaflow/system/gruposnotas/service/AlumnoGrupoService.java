package com.educaflow.system.gruposnotas.service;

import com.axelor.db.modelservice.ModelService;
import com.educaflow.system.gruposnotas.db.AlumnoGrupo;

public interface AlumnoGrupoService extends ModelService<AlumnoGrupo> {

    /**
     * CC-001 (momento lectura): nota media del alumno en el grupo. DELEGA en el getter transient del
     * dominio {@link AlumnoGrupo#getNotaMedia()}, que es la única fuente de verdad del cálculo.
     */
    String calcularNotaMedia(AlumnoGrupo alumnoGrupo);
}
