package com.educaflow.base.infrastructure.pdfgenerator.impl.documentotexto.modelo;

import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.modelo.Visibilidad;

import java.util.List;
import java.util.Optional;

/**
 * Un {@code <espacio>}: el hueco vertical en blanco que pide su {@code alto}, en puntos.
 *
 * @param campoFirma el nombre del campo de firma vacío que deja en su hueco, a todo su ancho y su alto.
 */
public record Espacio(double alto, Visibilidad visibilidad, Optional<String> campoFirma) implements Bloque {

    @Override
    public List<Elemento> hijos() {
        return List.of();
    }
}
