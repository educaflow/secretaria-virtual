package com.educaflow.subsystem.registrousuario;

import java.util.List;

public class RegistroException extends RuntimeException {

    private final List<String> errors;

    public RegistroException(String message) {
        super(message);
        this.errors = List.of(message);
    }

    public RegistroException(List<String> errors) {
        super(String.join(" ", errors));
        this.errors = List.copyOf(errors);
    }

    public List<String> getErrors() {
        return errors;
    }
}
