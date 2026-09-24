package com.educaflow.base.infrastructure.pdfgenerator.impl.texto;

/** Las cuatro variantes de Roboto con las que se dibuja todo el documento. */
public enum Fuente {
    REGULAR("Roboto-Regular.ttf"),
    CURSIVA("Roboto-Italic.ttf"),
    SEMINEGRITA("Roboto-SemiBold.ttf"),
    SEMINEGRITA_CURSIVA("Roboto-SemiBoldItalic.ttf");

    private final String fichero;

    Fuente(String fichero) {
        this.fichero = fichero;
    }

    public String getFichero() {
        return fichero;
    }
}
