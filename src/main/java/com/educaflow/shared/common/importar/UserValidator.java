package com.educaflow.shared.common.importar;

import com.axelor.auth.AuthService;
import com.axelor.auth.db.User;
import com.educaflow.base.infrastructure.importer.validators.DniValidator;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class UserValidator {

    @Inject
    private AuthService authService;

    @Inject
    private DniValidator dniValidator;

    private final Logger logger = LoggerFactory.getLogger(UserValidator.class);

    public Object validate(Object bean, Map<String, Object> context) {
        User user = (User) bean;

        if (!dniValidator.isValid(user.getDni())) {
            logger.warn("DNI {} no válido para el usuario: {}", user.getDni(), user.getName());
            return null;
        }
        user.setPassword(authService.encrypt(user.getDni()));
        return bean;
    }

}
