package com.educaflow.base.infrastructure.pdfgenerator;

import java.util.Arrays;

/** El idioma en el que se emite un documento. */
public enum Idioma {

    CASTELLANO("es"),
    VALENCIANO("ca");

    private final String codigo;

    Idioma(String codigo) {
        this.codigo = codigo;
    }

    /**
     * El idioma de un código de idioma de Axelor ({@code User.language}). Un código nulo o
     * desconocido es {@link #CASTELLANO}.
     */
    public static Idioma deCodigo(String codigo) {
        return Arrays.stream(values())
                .filter(idioma -> idioma.codigo.equals(codigo))
                .findFirst()
                .orElse(CASTELLANO);
    }
}
