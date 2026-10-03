package com.educaflow.base.infrastructure.pdfgenerator;

import java.util.Arrays;

public enum Idioma {

    CASTELLANO("es"),
    VALENCIANO("ca");

    private final String codigo;

    Idioma(String codigo) {
        this.codigo = codigo;
    }

    public static Idioma deCodigo(String codigo) {
        return Arrays.stream(values())
                .filter(idioma -> idioma.codigo.equals(codigo))
                .findFirst()
                .orElse(CASTELLANO);
    }
}
