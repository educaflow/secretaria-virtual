package com.educaflow.system.gruposnotas.service;

import com.educaflow.subsystem.sistemaeducativo.db.Modulo;
import com.educaflow.system.gruposnotas.db.Grupo;

/**
 * DTO de alta programática de {@link com.educaflow.system.gruposnotas.db.ModuloGrupo}.
 *
 * <p>El alta de un ModuloGrupo es programática: la invoca {@code GrupoServiceImpl} al generar los
 * módulos del grupo. No hay UI ni REST de alta, por lo que este DTO es la whitelist de campos: tanto
 * {@code grupo} como {@code modulo} los aporta el servidor (no el cliente).
 */
public record ModuloGrupoInsertDTO(Grupo grupo, Modulo modulo) {
}
