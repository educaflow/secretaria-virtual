package com.educaflow.base.infrastructure.pdfgenerator.impl.modelo;

import java.util.Optional;

/**
 * Los atributos {@code visible} y {@code siOculto} de un elemento. Sin {@code visible} el elemento se
 * dibuja siempre.
 */
public record Visibilidad(Optional<String> expresion, SiOculto siOculto) {

    public static final Visibilidad SIEMPRE = new Visibilidad(Optional.empty(), SiOculto.COLAPSAR);

    public static Visibilidad de(String visible, String siOculto) {
        if (visible.isEmpty()) {
            return SIEMPRE;
        }
        return new Visibilidad(Optional.of(visible), SiOculto.parse(siOculto));
    }
}
