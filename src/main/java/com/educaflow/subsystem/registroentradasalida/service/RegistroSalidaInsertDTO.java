package com.educaflow.subsystem.registroentradasalida.service;

import com.educaflow.subsystem.common.db.Centro;

import java.util.Objects;

public record RegistroSalidaInsertDTO(Centro centro,String asunto) {

    public RegistroSalidaInsertDTO {
        Objects.requireNonNull(centro, "centro no puede ser null");
        Objects.requireNonNull(asunto, "asunto no puede ser null");
    }

}
