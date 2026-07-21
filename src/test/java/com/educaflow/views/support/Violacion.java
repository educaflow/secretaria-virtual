package com.educaflow.views.support;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Una violación de una regla de vistas: fichero, ubicación (nombre de vista/acción/elemento) y
 * el detalle concreto. El mensaje de la regla lo pone {@link #assertNone}.
 */
public record Violacion(String fichero, String ubicacion, String detalle) {

    @Override
    public String toString() {
        return "  - " + fichero + " [" + ubicacion + "]: " + detalle;
    }

    /**
     * Falla el test si hay violaciones, listándolas todas (estilo ArchUnit: una regla reporta todos
     * sus incumplimientos de golpe). {@code regla} es el id + mensaje de la regla en view-rules.md.
     */
    public static void assertNone(String regla, List<Violacion> violaciones) {
        assertTrue(violaciones.isEmpty(), () ->
                regla + "\n" + violaciones.size() + " violación(es):\n"
                        + violaciones.stream().map(Violacion::toString).collect(Collectors.joining("\n")));
    }
}
