package com.educaflow.base.infrastructure.pdfgenerator.impl.comun.modelo;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Un {@code ${expresion}} dentro de un texto: la expresión Groovy cuyo valor se estampa ahí. */
public record ExpresionInline(String expresion) {

    /** La expresión no puede llevar {@code {} ni {@code }}. */
    public static final Pattern PATRON = Pattern.compile("\\$\\{([^{}]+)\\}");

    public static List<ExpresionInline> buscar(String texto) {
        List<ExpresionInline> inlines = new ArrayList<>();
        Matcher m = PATRON.matcher(texto);
        while (m.find()) {
            inlines.add(new ExpresionInline(m.group(1)));
        }
        return inlines;
    }
}
