package com.educaflow.base.infrastructure.pdfgenerator.impl.modelo;

import java.util.ArrayList;
import java.util.List;

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

    /** Las expresiones de los inline de los dos idiomas, en orden de aparición. */
    public List<String> expresionesInline() {
        List<String> expresiones = new ArrayList<>();
        for (ExpresionInline inline : ExpresionInline.buscar(valenciano)) {
            expresiones.add(inline.expresion());
        }
        for (ExpresionInline inline : ExpresionInline.buscar(castellano)) {
            expresiones.add(inline.expresion());
        }
        return expresiones;
    }
}
