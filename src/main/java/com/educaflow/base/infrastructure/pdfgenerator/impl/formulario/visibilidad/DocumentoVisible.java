package com.educaflow.base.infrastructure.pdfgenerator.impl.formulario.visibilidad;

import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.modelo.TextoBilingue;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

/** El documento tal como se va a dibujar: sin lo colapsado y con lo reservado marcado. */
public record DocumentoVisible(Optional<TextoBilingue> titulo, List<SeccionVisible> secciones) {

    /** Las expresiones de valor que hay que evaluar: las de las celdas que se dibujan de verdad. */
    public List<String> expresionesDeValor() {
        LinkedHashSet<String> expresiones = new LinkedHashSet<>();
        titulo.ifPresent(t -> expresiones.addAll(t.expresionesInline()));
        for (SeccionVisible seccion : secciones) {
            if (seccion.reservada()) {
                continue;
            }
            expresiones.addAll(seccion.titulo().expresionesInline());
            for (FilaVisible fila : seccion.filas()) {
                for (CeldaVisible celda : fila.celdas()) {
                    if (!celda.reservada()) {
                        expresiones.addAll(celda.celda().expresionesDeValor());
                    }
                }
            }
        }
        return new ArrayList<>(expresiones);
    }
}
