package com.educaflow.shared.registroentradasalida.db;

import com.educaflow.shared.configuracioncentro.db.Centro;

import java.util.Objects;

public record DatosRegistroEntrada(Centro centro,PersonaRegistro presentador, PersonaRegistro interesado,String numeroExpediente, String asunto) {

    public DatosRegistroEntrada {
        Objects.requireNonNull(centro, "centro no puede ser null");
        Objects.requireNonNull(presentador, "presentador no puede ser null");
        Objects.requireNonNull(interesado, "interesado no puede ser null");
    }

}
