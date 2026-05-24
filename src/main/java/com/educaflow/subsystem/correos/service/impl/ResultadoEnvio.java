package com.educaflow.subsystem.correos.service.impl;

/**
 * Resultado de un intento de envío de un correo.
 *
 * <p>Si {@code exito == true}, {@code motivo} es {@code null}. Si {@code exito == false},
 * {@code motivo} contiene la descripción saneada del fallo.
 */
public record ResultadoEnvio(boolean exito, String motivo) {

    public static ResultadoEnvio ok() {
        return new ResultadoEnvio(true, null);
    }

    public static ResultadoEnvio fallo(String motivo) {
        return new ResultadoEnvio(false, motivo);
    }
}
