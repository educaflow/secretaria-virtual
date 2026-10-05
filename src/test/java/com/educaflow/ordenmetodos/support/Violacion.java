package com.educaflow.ordenmetodos.support;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Una violación de una regla de orden: en qué fichero y línea, y qué está fuera de sitio.
 */
public record Violacion(String ruta, long linea, String detalle) {

    @Override
    public String toString() {
        return "  - " + ruta + ":" + linea + ": " + detalle;
    }

    /**
     * Falla el test si hay violaciones, listándolas todas (estilo ArchUnit: una regla reporta todos
     * sus incumplimientos de golpe en vez de parar en el primero).
     *
     * @param regla identificador y enunciado de la regla, tal cual lo documenta la clase de test.
     */
    public static void assertNone(String regla, List<Violacion> violaciones) {
        assertTrue(violaciones.isEmpty(), () ->
                regla + "\n" + violaciones.size() + " violación(es):\n"
                        + violaciones.stream().map(Violacion::toString).collect(Collectors.joining("\n")));
    }
}
