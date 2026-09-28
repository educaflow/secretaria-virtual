package com.educaflow.base.infrastructure.pdfgenerator.impl.documentotexto.modelo;

import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.modelo.Visibilidad;

import java.util.List;

/** Una {@code <lista>}: sus {@code <item>} con viñeta, uno debajo de otro. */
public record Lista(List<Texto> items, Visibilidad visibilidad) implements Bloque {

    @Override
    public List<Texto> hijos() {
        return items;
    }
}
