package com.educaflow.base.infrastructure.pdfgenerator.impl.comun.expresion;

import com.educaflow.base.infrastructure.evaluator.Evaluator;
import com.educaflow.base.util.Convert;

import java.util.List;
import java.util.Map;

/**
 * Los valores ya evaluados de las expresiones de valor de un documento: lo que se estampa en cada
 * campo, cada inline y cada casilla.
 */
public final class Valores {

    private final Map<String, Object> resultados;

    private Valores(Map<String, Object> resultados) {
        this.resultados = resultados;
    }

    /** Evalúa, en modo estricto, las expresiones de valor de las celdas que se dibujan de verdad. */
    public static Valores de(List<String> expresiones, Evaluator evaluator, Map<String, Object> contexto) {
        if (expresiones.isEmpty()) {
            return new Valores(Map.of());
        }
        return new Valores(evaluator.evaluateStrict(expresiones, contexto));
    }

    /** El texto que se estampa para una expresión: la conversión habitual de la aplicación; {@code null} es vacío. */
    public String texto(String expresion) {
        return Convert.objectToUserString(resultado(expresion));
    }

    /** Si la casilla de un {@code <check>} va marcada: su expresión tiene que devolver {@code Boolean}. */
    public boolean marcada(String expresion) {
        Object resultado = resultado(expresion);
        if (!(resultado instanceof Boolean marcada)) {
            throw new RuntimeException("La expresión del check '" + expresion + "' debe devolver Boolean y ha devuelto "
                    + (resultado == null ? "null" : resultado.getClass().getName() + " (" + resultado + ")"));
        }
        return marcada;
    }

    private Object resultado(String expresion) {
        if (!resultados.containsKey(expresion)) {
            throw new IllegalStateException("La expresión '" + expresion + "' no se evaluó: no es de ninguna celda visible");
        }
        return resultados.get(expresion);
    }
}
