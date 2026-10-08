package com.educaflow.base.infrastructure.pdfgenerator.impl.comun.modelo;

import com.educaflow.base.util.Idioma;

import java.util.List;
import java.util.stream.Stream;

/**
 * Los hijos {@code <valenciano>} y {@code <castellano>} de un elemento. Un idioma que no está, o
 * está vacío, es la cadena vacía: no se dibuja.
 */
public record TextoBilingue(String valenciano, String castellano) {

    public static final TextoBilingue VACIO = new TextoBilingue("", "");

    public boolean tieneValenciano() {
        return !valenciano.isEmpty();
    }

    public boolean tieneCastellano() {
        return !castellano.isEmpty();
    }

    public boolean vacio() {
        return !tieneValenciano() && !tieneCastellano();
    }

    public boolean tieneInline() {
        return ExpresionInline.PATRON.matcher(valenciano).find() || ExpresionInline.PATRON.matcher(castellano).find();
    }

    /** El texto de un idioma, para los documentos que se emiten en uno solo. */
    public String en(Idioma idioma) {
        return switch (idioma) {
            case CASTELLANO -> castellano;
            case VALENCIANO -> valenciano;
        };
    }

    /** Las expresiones de los inline de un solo idioma, en orden de aparición. */
    public List<String> expresionesInline(Idioma idioma) {
        return ExpresionInline.buscar(en(idioma)).stream().map(ExpresionInline::expresion).toList();
    }

    /** Las expresiones de los inline de los dos idiomas, en orden de aparición. */
    public List<String> expresionesInline() {
        return Stream.concat(ExpresionInline.buscar(valenciano).stream(), ExpresionInline.buscar(castellano).stream())
                .map(ExpresionInline::expresion)
                .toList();
    }
}
