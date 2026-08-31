package com.educaflow.subsystem.expedientes.services.eventmanager;

import java.util.List;
import java.util.Optional;

/**
 * La máquina de estados de UN tipo de expediente, vista sin conocer el tipo en compilación.
 *
 * <p>La única implementación es la clase {@code States} generada de cada tipo, que expone su único
 * ejemplar en {@code States.INSTANCE}. Es el puente que permite a {@code ExpedienteLocator} llegar a
 * la clase generada por reflexión de <b>clase</b>, sin reflexión de <b>métodos</b>: quien recibe un
 * {@code TipoExpedienteStates} hace llamadas normales.
 *
 * <p>Se llega a ella desde la entidad: {@code expediente.getTipoExpediente().getTipoExpedienteStates()}.
 */
public interface TipoExpedienteStates {

    /** La fase con ese código, o vacío. Comparación estricta, sin normalizar. */
    Optional<Phase> getPhase(String phaseCode);

    /** El estado de esa fase con ese código, o vacío. Comparación estricta. */
    Optional<State> getState(String phaseCode, String stateCode);

    /** Las fases, en orden de declaración en el XML. */
    List<Phase> getPhases();

    /** TODOS los estados del tipo, de todas las fases, en orden de declaración. */
    List<State> getStates();

    /** El estado inicial. Resuelto en generación: hay exactamente uno. */
    State getInitialState();
}
