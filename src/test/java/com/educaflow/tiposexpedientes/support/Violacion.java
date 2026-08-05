package com.educaflow.tiposexpedientes.support;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Una violación de una regla de tipos de expediente: de qué tipo es, en qué fichero, y el detalle
 * concreto de lo que falta o sobra.
 *
 * <p>A diferencia de la {@code Violacion} de los tests de vistas, el detalle puede ser
 * <b>multilínea</b>: estas reglas ofrecen el código fuente del método que falta listo para pegar,
 * que es la calidad de diagnóstico que tenía el antiguo {@code check()} del build y que hay que
 * conservar.
 */
public record Violacion(String tipoExpediente, String fichero, String detalle) {

    @Override
    public String toString() {
        return "  - [" + tipoExpediente + "] " + fichero + ": " + detalle;
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
