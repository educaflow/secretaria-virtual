package com.educaflow.base.infrastructure.pdfgenerator.impl.modelo;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

/** El XML resuelto de un documento, tal cual lo define su autor: sin evaluar nada todavía. */
public record Documento(Optional<TextoBilingue> titulo, List<Seccion> secciones) {

    /**
     * Todas las expresiones Groovy del documento ({@code nombreCampo}, inline de ambos idiomas y
     * {@code visible}), sin duplicados y en orden de aparición.
     */
    public List<String> expresiones() {
        LinkedHashSet<String> expresiones = new LinkedHashSet<>();
        titulo.ifPresent(t -> expresiones.addAll(t.expresionesInline()));
        for (Seccion seccion : secciones) {
            seccion.visibilidad().expresion().ifPresent(expresiones::add);
            expresiones.addAll(seccion.titulo().expresionesInline());
            for (Fila fila : seccion.filas()) {
                fila.visibilidad().expresion().ifPresent(expresiones::add);
                for (Celda celda : fila.celdas()) {
                    celda.visibilidad().expresion().ifPresent(expresiones::add);
                    expresiones.addAll(celda.expresionesDeValor());
                }
            }
        }
        return new ArrayList<>(expresiones);
    }
}
