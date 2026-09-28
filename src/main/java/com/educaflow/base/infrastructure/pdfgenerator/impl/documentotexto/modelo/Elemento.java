package com.educaflow.base.infrastructure.pdfgenerator.impl.documentotexto.modelo;

import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.modelo.Visibilidad;

import java.util.ArrayList;
import java.util.List;

/** Todo lo que el XML de un {@code <documentoTexto>} declara con visibilidad propia. */
public sealed interface Elemento permits Bloque, Fila {

    Visibilidad visibilidad();

    List<? extends Elemento> hijos();

    /** Las expresiones de los {@code ${expresion}} de sus textos; un elemento sin textos no tiene. */
    default List<String> expresionesInline() {
        return List.of();
    }

    /** Sus expresiones y las de sus hijos, en orden de aparición: primero su {@code visible}. */
    default List<String> expresiones() {
        List<String> expresiones = new ArrayList<>();
        visibilidad().expresion().ifPresent(expresiones::add);
        expresiones.addAll(expresionesInline());
        hijos().forEach(hijo -> expresiones.addAll(hijo.expresiones()));
        return expresiones;
    }
}
