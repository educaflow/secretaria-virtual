package com.educaflow.shared.registroentradasalida.service;

import com.educaflow.shared.common.db.Centro;

import java.util.Objects;

public record DatosRegistroEntrada(Centro centro,PersonaRegistro solicitante, PersonaRegistro interesado,String numeroExpediente, String asunto) {

    public DatosRegistroEntrada {
        Objects.requireNonNull(centro, "centro no puede ser null");
        Objects.requireNonNull(solicitante, "solicitante no puede ser null");
        Objects.requireNonNull(interesado, "interesado no puede ser null");
    }

}
