package com.educaflow.base.infrastructure.pdfgenerator.impl.documentotexto.visibilidad;

import com.educaflow.base.infrastructure.pdfgenerator.Idioma;
import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.modelo.TextoBilingue;
import com.educaflow.base.infrastructure.pdfgenerator.impl.documentotexto.modelo.Texto;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * El documento tal como se va a dibujar: sin lo colapsado y con lo reservado marcado. El título de
 * la cabecera no pasa por aquí —es obligatorio y no admite {@code visible}—, pero viaja con el
 * documento porque sus inline sí hay que evaluarlos.
 */
public record DocumentoTextoVisible(TextoBilingue titulo, List<ElementoVisible> cuerpo) {

    /** Las expresiones de valor que hay que evaluar: los inline del idioma que se emite. */
    public List<String> expresionesDeValor(Idioma idioma) {
        LinkedHashSet<String> expresiones = new LinkedHashSet<>(titulo.expresionesInline(idioma));
        recorrer(cuerpo, idioma, expresiones);
        return new ArrayList<>(expresiones);
    }

    private static void recorrer(List<ElementoVisible> elementos, Idioma idioma, Collection<String> expresiones) {
        for (ElementoVisible elemento : elementos) {
            if (elemento.reservado()) {
                continue;
            }
            if (elemento.elemento() instanceof Texto texto) {
                expresiones.addAll(texto.textos().expresionesInline(idioma));
            }
            recorrer(elemento.hijos(), idioma, expresiones);
        }
    }
}
