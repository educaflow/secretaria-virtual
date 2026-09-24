package com.educaflow.base.infrastructure.pdfgenerator.impl.modelo;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Un {@code ${expresion;n}} dentro de un texto: la expresión Groovy y el ancho en columnas de la
 * rejilla de 12 del hueco que se deja cuando su valor está vacío.
 */
public record ExpresionInline(String expresion, double columnas) {

    /** La expresión no puede llevar {@code ;}, {@code {} ni {@code }}; el ancho admite decimales. */
    public static final Pattern PATRON = Pattern.compile("\\$\\{([^;{}]+);([0-9]+(?:\\.[0-9]+)?)\\}");

    public static List<ExpresionInline> buscar(String texto) {
        List<ExpresionInline> inlines = new ArrayList<>();
        Matcher m = PATRON.matcher(texto);
        while (m.find()) {
            inlines.add(new ExpresionInline(m.group(1), Double.parseDouble(m.group(2))));
        }
        return inlines;
    }
}
