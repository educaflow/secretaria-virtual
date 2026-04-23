package com.educaflow.subsystem.registroentradasalida.service;

import com.educaflow.subsystem.common.db.Centro;

import java.util.Objects;

public record RegistroEntradaInsertDTO(Centro centro, PersonaRegistro solicitante, PersonaRegistro interesado, String numeroExpediente, String asunto) {

    public RegistroEntradaInsertDTO {
        Objects.requireNonNull(centro, "centro no puede ser null");
        Objects.requireNonNull(solicitante, "solicitante no puede ser null");
        Objects.requireNonNull(interesado, "interesado no puede ser null");
        Objects.requireNonNull(asunto, "asunto no puede ser null");
    }

}
