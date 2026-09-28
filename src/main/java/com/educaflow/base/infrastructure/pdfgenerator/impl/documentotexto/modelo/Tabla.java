package com.educaflow.base.infrastructure.pdfgenerator.impl.documentotexto.modelo;

import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.modelo.Visibilidad;

import java.util.List;

/** Una {@code <tabla>}: una rejilla sin bordes cuyo ancho se reparte a partes iguales. */
public record Tabla(int columnas, List<Fila> filas, Visibilidad visibilidad) implements Bloque {

    @Override
    public List<Fila> hijos() {
        return filas;
    }
}
