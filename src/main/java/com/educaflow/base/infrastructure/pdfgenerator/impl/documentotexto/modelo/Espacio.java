package com.educaflow.base.infrastructure.pdfgenerator.impl.documentotexto.modelo;

import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.modelo.Visibilidad;

import java.util.List;

/** Un {@code <espacio>}: el hueco vertical en blanco que pide su {@code alto}, en puntos. */
public record Espacio(double alto, Visibilidad visibilidad) implements Bloque {

    @Override
    public List<Elemento> hijos() {
        return List.of();
    }
}
