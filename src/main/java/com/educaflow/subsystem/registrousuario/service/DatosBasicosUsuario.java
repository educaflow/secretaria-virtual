package com.educaflow.subsystem.registrousuario.service;

import java.util.Objects;

public record DatosBasicosUsuario(
        String nombre,
        String apellidos,
        String password,
        String passwordRepeat,
        String idioma
) {

    public DatosBasicosUsuario {
        Objects.requireNonNull(nombre, "nombre no puede ser null");
        Objects.requireNonNull(apellidos, "apellidos no pueden ser null");
        Objects.requireNonNull(password, "password no puede ser null");
        Objects.requireNonNull(passwordRepeat, "passwordRepeat no puede ser null");
        Objects.requireNonNull(idioma, "idioma no puede ser null");

        if(nombre.isBlank()) {
            throw new IllegalArgumentException("nombre no puede ser blank");
        }
        if(apellidos.isBlank()) {
            throw new IllegalArgumentException("apellidos no puede ser blank");
        }
        if(password.isBlank()) {
            throw new IllegalArgumentException("password no puede ser blank");
        }
        if(passwordRepeat.isBlank()) {
            throw new IllegalArgumentException("passwordRepeat no puede ser blank");
        }
        if(idioma.isBlank()) {
            throw new IllegalArgumentException("idioma no puede ser blank");
        }
         if(!password.equals(passwordRepeat)) {
             throw new IllegalArgumentException("password y passwordRepeat deben ser iguales");
         }
         if(password.length() < 8) {
             throw new IllegalArgumentException("password debe tener al menos 8 caracteres");
         }
    }
}
