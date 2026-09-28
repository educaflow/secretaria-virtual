package com.educaflow.base.infrastructure.pdfgenerator.impl.comun.expresion;

import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.modelo.SiOculto;
import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.modelo.Visibilidad;

import java.util.Map;

/** Lo que le pasa a un elemento una vez evaluado su {@code visible}. */
public enum Presencia {
    VISIBLE,
    RESERVADA,
    COLAPSADA;

    public static Presencia de(Visibilidad visibilidad, Map<String, Object> resultados) {
        if (visibilidad.expresion().isEmpty()) {
            return VISIBLE;
        }
        String expresion = visibilidad.expresion().get();
        Object resultado = resultados.get(expresion);
        if (!(resultado instanceof Boolean visible)) {
            throw new RuntimeException("La expresión de visible '" + expresion + "' debe devolver Boolean y ha devuelto "
                    + (resultado == null ? "null" : resultado.getClass().getName() + " (" + resultado + ")"));
        }
        if (visible) {
            return VISIBLE;
        }
        return visibilidad.siOculto() == SiOculto.RESERVAR ? RESERVADA : COLAPSADA;
    }
}
