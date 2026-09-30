package com.educaflow.base.infrastructure.sms;

import com.educaflow.base.util.TextUtil;

/**
 * SMS a enviar. El teléfono va en formato E.164 (p. ej. {@code +34600000000}).
 *
 * <p>No lleva remitente: el número desde el que se envía es de configuración y se le pasa al
 * {@link SmsSender} al crearlo.
 */
public record Sms(String telefonoDestino, String mensaje) {

    public Sms {
        TextUtil.requireNonBlank(telefonoDestino, "telefonoDestino no puede ser null ni blank");
        TextUtil.requireNonBlank(mensaje, "mensaje no puede ser null ni blank");
    }
}
