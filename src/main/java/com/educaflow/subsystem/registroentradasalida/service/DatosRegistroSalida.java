package com.educaflow.subsystem.registroentradasalida.service;

import com.educaflow.subsystem.common.db.Centro;

import java.util.Objects;

public record DatosRegistroSalida(Centro centro) {

    public DatosRegistroSalida {
        Objects.requireNonNull(centro, "centro no puede ser null");
    }

}
