package com.educaflow.subsystem.tramitador.tramitacion.internal.fixtures.convariable;

import com.educaflow.subsystem.expedientes.db.Expediente;
import com.educaflow.subsystem.tramitador.tramitacion.eventmanager.InitialEventManager;

/** Fixture de ExpedienteLocatorTest: el parámetro de tipo es una variable, no una clase. */
public abstract class InitialEventManagerImpl<T extends Expediente> implements InitialEventManager<T> {
}
