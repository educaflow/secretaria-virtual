package com.educaflow.base.infrastructure.pdfgenerator.impl.documentotexto.modelo;

import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.modelo.Visibilidad;

import java.util.List;

/** Una {@code <fila>} de una tabla: una celda por columna, y mide lo que la más alta. */
public record Fila(List<Bloque> celdas, Visibilidad visibilidad) implements Elemento {

    @Override
    public List<Bloque> hijos() {
        return celdas;
    }
}
