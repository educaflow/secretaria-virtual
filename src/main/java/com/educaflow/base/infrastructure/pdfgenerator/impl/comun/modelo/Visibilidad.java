package com.educaflow.base.infrastructure.pdfgenerator.impl.comun.modelo;

import java.util.Optional;

public record Visibilidad(Optional<String> expresion, SiOculto siOculto) {

    public static final Visibilidad SIEMPRE = new Visibilidad(Optional.empty(), SiOculto.COLAPSAR);

    public static Visibilidad de(String visible, String siOculto) {
        if (visible.isEmpty()) {
            return SIEMPRE;
        }
        return new Visibilidad(Optional.of(visible), SiOculto.parse(siOculto));
    }
}
