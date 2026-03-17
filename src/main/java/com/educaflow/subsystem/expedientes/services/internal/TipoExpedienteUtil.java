package com.educaflow.subsystem.expedientes.services.internal;

import com.axelor.inject.Beans;
import com.educaflow.subsystem.expedientes.db.TipoExpediente;
import com.educaflow.subsystem.expedientes.services.eventmanager.EventManager;
import com.educaflow.subsystem.expedientes.services.validation.StateEventValidator;

public class TipoExpedienteUtil {

    public static EventManager getEventManager(TipoExpediente tipoExpediente) {
        try {
            String fqcnEventManager = tipoExpediente.getFqcnEventManager();
            if (fqcnEventManager == null || fqcnEventManager.isEmpty()) {
                throw new RuntimeException("No existe el fqcnEventManager para el tipo de expediente: " + tipoExpediente.getName());
            }
            Class<EventManager> eventManagerClass = (Class<EventManager>) Class.forName(fqcnEventManager);

            EventManager eventManager = (EventManager) Beans.get(eventManagerClass);

            return eventManager;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static StateEventValidator getStateEventValidator(TipoExpediente tipoExpediente) {
        try {
            String fqcnStateEventValidator = tipoExpediente.getFqcnStateEventValidator();
            if (fqcnStateEventValidator == null || fqcnStateEventValidator.isEmpty()) {
                throw new RuntimeException("No existe el fqcnStateEventValidator para el tipo de expediente: " + tipoExpediente.getName());
            }
            Class<StateEventValidator> stateEventValidationClass = (Class<StateEventValidator>) Class.forName(fqcnStateEventValidator);

            StateEventValidator stateEventValidator = Beans.get(stateEventValidationClass);

            return stateEventValidator;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

}
