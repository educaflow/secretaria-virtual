package com.educaflow.subsystem.registrousuario.service;

import com.educaflow.base.util.TextUtil;

public record DatosBasicosUsuario(
        String nombre,
        String apellidos,
        String password,
        String passwordRepeat,
        String idioma
) {

    public DatosBasicosUsuario {
        TextUtil.requireNonBlank(nombre, "nombre no puede ser null ni blank");
        TextUtil.requireNonBlank(apellidos, "apellidos no puede ser null ni blank");
        TextUtil.requireNonBlank(password, "password no puede ser null ni blank");
        TextUtil.requireNonBlank(passwordRepeat, "passwordRepeat no puede ser null ni blank");
        TextUtil.requireNonBlank(idioma, "idioma no puede ser null ni blank");

         if(!password.equals(passwordRepeat)) {
             throw new IllegalArgumentException("password y passwordRepeat deben ser iguales");
         }
         if(password.length() < 8) {
             throw new IllegalArgumentException("password debe tener al menos 8 caracteres");
         }
    }
}
