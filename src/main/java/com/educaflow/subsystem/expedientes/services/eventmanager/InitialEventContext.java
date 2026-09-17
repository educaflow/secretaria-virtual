package com.educaflow.subsystem.expedientes.services.eventmanager;


import com.educaflow.subsystem.expedientes.db.ContextoTramitacion;
import com.educaflow.subsystem.expedientes.db.Expediente;
import com.educaflow.subsystem.expedientes.db.TipoExpediente;
import com.educaflow.subsystem.expedientes.services.internal.ExpedienteUtil;

import java.util.Objects;

/**
 * El contexto del evento inicial, el equivalente para el alta de {@link EventContext}: el tipo de
 * expediente que se crea, el perfil con el que se crea y el {@link ContextoTramitacion}, que dice cómo
 * se crea (en qué centro, si se registra una solicitud entregada en papel o si se presenta en
 * representación).
 */
public class InitialEventContext<T extends Expediente> {
    final private T expediente;
    final private ContextoTramitacion contextoTramitacion;

    public InitialEventContext(T expediente, ContextoTramitacion contextoTramitacion) {
        this.expediente = Objects.requireNonNull(expediente, "expediente no puede ser null");
        this.contextoTramitacion = Objects.requireNonNull(contextoTramitacion, "contextoTramitacion no puede ser null");
    }



    public ContextoTramitacion getContextoTramitacion() {
        return contextoTramitacion;
    }

    public T getExpediente() {
        return expediente;
    }

    public void updateState(State state) {
        Objects.requireNonNull(expediente, "No es posible fijar el estado ya que aun no existe el expediente");
        ExpedienteUtil.updateState(expediente, state);
    }

    @Override
    public String toString() {
        return "InitialEventContext [expediente=" + expediente + ", contextoTramitacion=" + contextoTramitacion + "]";
    }
}
