package com.educaflow.system.gestioncentro.service.validators;

import com.axelor.auth.AuthService;
import com.axelor.auth.db.User;
import com.educaflow.base.util.DniUtil;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class GestionCentroUserValidator {

    @Inject
    private AuthService authService;

    private final Logger logger = LoggerFactory.getLogger(GestionCentroUserValidator.class);
    private final String ImportLoggerName = "importLogger";

    public Object validate(Object bean, Map<String, Object> context) {
        User user = (User) bean;
        List<String> importLogger = (List) context.get(ImportLoggerName);

        if (importLogger == null) {
            logger.warn("ImportLogger no encontrado en el contexto. No se podrán registrar los errores de validación.");
        }

        if (!DniUtil.isValid(user.getDni())) {
            if (importLogger != null) {
                importLogger.add("DNI " + user.getDni() + " no válido para el usuario: " + user.getNombre() + " " + user.getApellidos());
            }
            return null;
        }

        return bean;
    }
}