package com.educaflow.subsystem.tramitador.service;

import com.axelor.i18n.I18n;
import com.educaflow.subsystem.expedientes.db.Expediente;
import com.educaflow.subsystem.expedientes.db.Profile;

public record VistaExpediente(String viewName, Class<? extends Expediente> modelClass, Expediente expediente, Profile profile) {

    /** El título de la pestaña que abre la vista: el número de expediente y el nombre del tipo. */
    public String title() {
        return expediente.getNumeroExpediente() + "-" + I18n.get(expediente.getTipoExpediente().getName());
    }

}
