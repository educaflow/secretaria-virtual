package com.educaflow.subsystem.registroentradasalida.service;

import com.educaflow.base.util.TextUtil;
import com.educaflow.subsystem.common.db.Centro;

import java.util.Objects;

/**
 * @param idioma código del idioma ({@code "es"} / {@code "ca"}) en que se emite el resguardo de
 *               presentación: el de quien presenta (en un expediente, {@code Expediente.idioma}),
 *               no el del usuario que lo registra. Desconocido o {@code null} → castellano.
 */
public record RegistroEntradaInsertDTO(Centro centro, PersonaRegistro solicitante, PersonaRegistro interesado, String numeroExpediente, String asunto, String idioma) {

    public RegistroEntradaInsertDTO {
        Objects.requireNonNull(centro, "centro no puede ser null");
        Objects.requireNonNull(solicitante, "solicitante no puede ser null");
        Objects.requireNonNull(interesado, "interesado no puede ser null");
        TextUtil.requireNonBlank(asunto, "asunto no puede ser null ni blank");
    }

}
