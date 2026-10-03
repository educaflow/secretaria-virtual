package com.educaflow.base.infrastructure.pdfgenerator.impl.formulario.visibilidad;

import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.modelo.TextoBilingue;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/** El documento tal como se va a dibujar: sin lo colapsado y con lo reservado marcado. */
public record DocumentoVisible(Optional<TextoBilingue> titulo, List<SeccionVisible> secciones) {

    /** Las expresiones de valor que hay que evaluar: las de las celdas que se dibujan de verdad. */
    public List<String> expresionesDeValor() {
        Stream<String> deLasSecciones = secciones.stream()
                .filter(seccion -> !seccion.reservada())
                .flatMap(seccion -> Stream.concat(
                        seccion.titulo().expresionesInline().stream(),
                        seccion.filas().stream()
                                .flatMap(fila -> fila.celdas().stream())
                                .filter(celda -> !celda.reservada())
                                .flatMap(celda -> celda.celda().expresionesDeValor().stream())));
        return Stream.concat(titulo.stream().flatMap(t -> t.expresionesInline().stream()), deLasSecciones)
                .distinct()
                .toList();
    }
}
