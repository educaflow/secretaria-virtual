package com.educaflow.base.infrastructure.pdfgenerator.impl.comun.texto;

public enum Familia {
    ROBOTO("Roboto"),
    MONTSERRAT("Montserrat");

    private final String nombre;

    Familia(String nombre) {
        this.nombre = nombre;
    }

    public String getFichero(Fuente fuente) {
        return nombre + "-" + fuente.getVariante() + ".ttf";
    }
}
