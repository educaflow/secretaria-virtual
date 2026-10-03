package com.educaflow.base.infrastructure.pdfgenerator.impl.formulario.modelo;

import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.modelo.TextoBilingue;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/** El XML resuelto de un documento, tal cual lo define su autor: sin evaluar nada todavía. */
public record Documento(Optional<TextoBilingue> titulo, List<Seccion> secciones) {

    /**
     * Todas las expresiones Groovy del documento ({@code nombreCampo}, inline de ambos idiomas y
     * {@code visible}), sin duplicados y en orden de aparición.
     */
    public List<String> expresiones() {
        Stream<String> deLasSecciones = secciones.stream()
                .flatMap(seccion -> Stream.of(
                                seccion.visibilidad().expresion().stream(),
                                seccion.titulo().expresionesInline().stream(),
                                seccion.filas().stream().flatMap(fila -> Stream.concat(
                                        fila.visibilidad().expresion().stream(),
                                        fila.celdas().stream().flatMap(celda -> Stream.concat(
                                                celda.visibilidad().expresion().stream(),
                                                celda.expresionesDeValor().stream())))))
                        .flatMap(expresiones -> expresiones));
        return Stream.concat(titulo.stream().flatMap(t -> t.expresionesInline().stream()), deLasSecciones)
                .distinct()
                .toList();
    }
}
