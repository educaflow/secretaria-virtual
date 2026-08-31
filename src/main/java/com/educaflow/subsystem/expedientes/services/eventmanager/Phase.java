package com.educaflow.subsystem.expedientes.services.eventmanager;

import java.util.List;

/**
 * Una fase de un tipo de expediente: la agrupación de estados cuyo PhaseEventManager,
 * StateEventValidator y views.xml viven juntos en la subcarpeta de la fase.
 *
 * <p>La implementa el enum privado {@code PhaseInternal} de la clase {@code States} generada de
 * cada tipo. Una fase se referencia siempre por el alias público de esa clase
 * ({@code States.RECEPCION}), tipado con esta interfaz.
 *
 * <p>No expone el tipo de expediente al que pertenece: la clase generada es una proyección pura del
 * XML, sin imports de entidades, y el camino de vuelta fase → tipo no tiene consumidor.
 */
public interface Phase {

    /** El {@code name} de la fase en el XML, en UPPER_SNAKE_CASE: {@code "RECEPCION"}. */
    String getCode();

    /** El {@code title} del XML, o el {@code name} humanizado si no lo hay: {@code "Recepción"}. */
    String getName();

    /** Los estados de la fase, en orden de declaración. Lista inmutable, precalculada. */
    List<State> getStates();
}
