package com.educaflow.base.infrastructure.pdfgenerator.impl.formulario.modelo;

import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.modelo.TextoBilingue;
import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.modelo.Visibilidad;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Un {@code <campo>}, {@code <check>} o {@code <texto>} de una fila.
 *
 * @param nombreCampo la expresión del valor; vacío en un {@link TipoCelda#TEXTO}.
 * @param colspan     columnas de la rejilla de 12 que ocupa (admite decimales).
 * @param rowSpan     multiplicador del alto mínimo (≥ 1).
 */
public record Celda(TipoCelda tipo, Optional<String> nombreCampo, double colspan, double rowSpan,
                    TextoBilingue textos, Visibilidad visibilidad) {

    /** El colspan en unidades de la rejilla: 1200 unidades = 12 columnas. */
    public int unidades() {
        return (int) Math.round(colspan * 100);
    }

    /** Las expresiones de valor de la celda: el {@code nombreCampo} y los inline de sus textos. */
    public List<String> expresionesDeValor() {
        List<String> expresiones = new ArrayList<>();
        nombreCampo.ifPresent(expresiones::add);
        expresiones.addAll(textos.expresionesInline());
        return expresiones;
    }
}
