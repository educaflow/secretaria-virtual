package com.educaflow.base.infrastructure.pdfgenerator.impl.documentotexto.maquetacion;

import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.dibujo.Lienzo;

import java.util.function.ObjDoubleConsumer;

/**
 * Una franja horizontal ya medida y lo que se dibuja en ella desde el borde superior que se le pase.
 * Es la unidad que nunca se parte entre páginas: una línea de un párrafo o de una lista, o la fila
 * entera de una tabla.
 */
public record Renglon(double alto, ObjDoubleConsumer<Lienzo> dibujo) {

    public static Renglon enBlanco(double alto) {
        return new Renglon(alto, (lienzo, top) -> { });
    }

    public Renglon sinDibujo() {
        return enBlanco(alto);
    }
}
