package com.educaflow.system.gruposnotas.service;

import com.educaflow.system.gruposnotas.db.AlumnoGrupo;
import com.educaflow.system.gruposnotas.db.ModuloGrupo;

/**
 * DTO de alta programática de {@link com.educaflow.system.gruposnotas.db.Nota}.
 *
 * <p>El alta de una Nota es programática: la invoca {@code AlumnoGrupoServiceImpl} al dar de alta un
 * alumno en el grupo (una nota NO_EVALUADO por módulo del grupo). No hay UI ni REST de alta, por lo
 * que este DTO es la whitelist de campos: tanto {@code moduloGrupo} como {@code alumnoGrupo} los
 * aporta el servidor (no el cliente).
 */
public record NotaInsertDTO(ModuloGrupo moduloGrupo, AlumnoGrupo alumnoGrupo) {
}
