package com.educaflow.base.infrastructure.pdfgenerator.impl.comun.texto;

/** Las cuatro variantes con las que se dibuja un documento, dentro de su {@link Familia}. */
public enum Fuente {
    REGULAR("Regular"),
    CURSIVA("Italic"),
    SEMINEGRITA("SemiBold"),
    SEMINEGRITA_CURSIVA("SemiBoldItalic");

    private final String variante;

    Fuente(String variante) {
        this.variante = variante;
    }

    public String getVariante() {
        return variante;
    }
}
