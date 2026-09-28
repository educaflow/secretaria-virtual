package com.educaflow.base.infrastructure.pdfgenerator.impl.documentotexto.visibilidad;

import com.educaflow.base.infrastructure.pdfgenerator.impl.documentotexto.modelo.Elemento;

import java.util.List;

/**
 * Un elemento que va a ocupar sitio en el PDF.
 *
 * @param reservado {@code true} si está oculto con {@code siOculto="reservar"} (él o alguno de sus
 *                  padres): deja su hueco vertical en blanco y sus inline no se evalúan.
 */
public record ElementoVisible(Elemento elemento, boolean reservado, List<ElementoVisible> hijos) {
}
