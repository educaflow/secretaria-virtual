package com.educaflow.base.infrastructure.pdfgenerator.impl.comun.modelo;

import java.util.List;
import java.util.regex.Pattern;

/** Un {@code ${expresion}} dentro de un texto: la expresión Groovy cuyo valor se estampa ahí. */
public record ExpresionInline(String expresion) {

    /** La expresión no puede llevar {@code {} ni {@code }}. */
    public static final Pattern PATRON = Pattern.compile("\\$\\{([^{}]+)\\}");

    public static List<ExpresionInline> buscar(String texto) {
        return PATRON.matcher(texto).results().map(r -> new ExpresionInline(r.group(1))).toList();
    }
}
