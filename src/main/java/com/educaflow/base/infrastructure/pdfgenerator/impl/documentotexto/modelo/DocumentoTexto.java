package com.educaflow.base.infrastructure.pdfgenerator.impl.documentotexto.modelo;

import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.modelo.TextoBilingue;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * El XML resuelto de un documento en prosa, tal cual lo define su autor: sin evaluar nada todavía.
 *
 * <p>El {@code titulo} es el de la cabecera de la primera página, no un encabezado del texto: los
 * encabezados del cuerpo son {@code <parrafo>} con {@code negrita} y {@code mayusculas}.
 */
public record DocumentoTexto(TextoBilingue titulo, List<Bloque> cuerpo) {

    /**
     * Todas las expresiones Groovy del documento (los inline de ambos idiomas y los {@code visible}),
     * sin duplicados y en orden de aparición.
     */
    public List<String> expresiones() {
        LinkedHashSet<String> expresiones = new LinkedHashSet<>(titulo.expresionesInline());
        cuerpo.forEach(bloque -> expresiones.addAll(bloque.expresiones()));
        return new ArrayList<>(expresiones);
    }
}
