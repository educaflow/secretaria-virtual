package com.educaflow.subsystem.tramitador.tramitacion.internal.fixtures.conentidad;

import com.educaflow.subsystem.expedientes.db.Expediente;
import com.educaflow.subsystem.tramitador.tramitacion.eventmanager.InitialEventManager;

import java.io.Serializable;

/**
 * Fixture de ExpedienteLocatorTest: declara la entidad, pero detrás de una interfaz sin parámetros
 * de tipo y de otra parametrizada que no es InitialEventManager, para que el bucle las salte.
 */
public abstract class InitialEventManagerImpl implements Serializable, Comparable<InitialEventManagerImpl>, InitialEventManager<Expediente> {
}
