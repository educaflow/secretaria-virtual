package com.educaflow.base.infrastructure.evaluator;


import java.util.List;
import java.util.Map;

public interface Evaluator {

    /**
     * Evalúa las expresiones y devuelve el valor de cada una por su texto. Una expresión que falla al
     * evaluarse no está en el mapa: el fallo se anota en el log y se sigue con las demás.
     */
    Map<String,Object> evaluate(List<String> expressions, Map<String,Object> context);

    /**
     * Como {@link #evaluate}, pero la primera expresión que falla aborta con una {@link RuntimeException}
     * que dice qué expresión era y por qué. Para los usos en los que un valor que falta cambia el
     * significado de lo que se produce (p.ej. un documento) y no debe salir en silencio.
     */
    Map<String,Object> evaluateStrict(List<String> expressions, Map<String,Object> context);

}
