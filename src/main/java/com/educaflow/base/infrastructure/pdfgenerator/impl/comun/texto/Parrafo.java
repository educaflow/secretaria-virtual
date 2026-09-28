package com.educaflow.base.infrastructure.pdfgenerator.impl.comun.texto;

import java.util.List;

/**
 * Un texto ya ajustado a un ancho: sus líneas y el alto de cada una. Un texto vacío es el párrafo
 * {@link #VACIO}, sin líneas y de alto cero.
 */
public record Parrafo(List<Linea> lineas, double altoLinea) {

    public static final Parrafo VACIO = new Parrafo(List.of(), 0);

    public double alto() {
        return lineas.size() * altoLinea;
    }

    public boolean vacio() {
        return lineas.isEmpty();
    }
}
