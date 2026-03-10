package com.educaflow.shared.registroentradasalida.service;

import com.educaflow.shared.common.db.Centro;

import java.util.Objects;

public record DatosRegistroSalida(Centro centro) {

    public DatosRegistroSalida {
        Objects.requireNonNull(centro, "centro no puede ser null");
    }

}
