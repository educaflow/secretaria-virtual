package com.educaflow.subsystem.tramitador.tramitacion.eventmanager;


import com.educaflow.subsystem.expedientes.db.Expediente;
import com.educaflow.subsystem.tramitador.tramitacion.util.ExpedienteUtil;

import java.util.Objects;

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
