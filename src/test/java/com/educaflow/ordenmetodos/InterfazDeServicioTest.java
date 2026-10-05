package com.educaflow.ordenmetodos;

import com.educaflow.ordenmetodos.support.FuenteJava;
import com.educaflow.ordenmetodos.support.FuenteJava.Metodo;
import com.educaflow.ordenmetodos.support.Orden;
import com.educaflow.ordenmetodos.support.Violacion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * El orden de los métodos de las interfaces de servicio ({@code *Service.java} que sean
 * {@code interface}).
 *
 * <p>La interfaz es la que <b>fija el orden</b>: su {@code *ServiceImpl} y su controlador lo siguen
 * ({@link ServiceImplTest}, {@link ControladorTest}). Las tres piezas se leen entonces igual, y
 * quien busca la validación de una acción sabe dónde está sin recorrer el fichero.
 *
 * <ul>
 *   <li><b>I1</b>: primero todas las acciones, después todas las validaciones ({@code validate*}) y
 *       por último todos los {@code allowProperties*}.</li>
 *   <li><b>I2</b>: las validaciones y los {@code allowProperties*} van en el mismo orden que sus
 *       acciones.</li>
 *   <li><b>I3</b>: los bloques se separan con exactamente dos líneas en blanco, sin headers
 *       {@code /*****…*****}{@code /}.</li>
 * </ul>
 *
 * <p>La regla en prosa está en el skill {@code k-sistemas} ({@code servicios.md}), que debe
 * mantenerse coherente con estos tests.
 *
 * <p><b>Estos tests se escriben A MANO.</b> Este fichero es la fuente de verdad y se edita
 * directamente.
 */
class InterfazDeServicioTest {

    private static final int LINEAS_ENTRE_BLOQUES = 2;

    private static final List<FuenteJava> INTERFACES = FuenteJava.conSufijo("Service.java").stream()
            .filter(FuenteJava::esInterfaz)
            .toList();

    @Test
    @DisplayName("I1: una interfaz de servicio declara primero las acciones, luego las validaciones y luego los allowProperties")
    void i1_losBloquesVanEnOrden() {
        List<Violacion> violaciones = new ArrayList<>();

        for (FuenteJava interfaz : INTERFACES) {
            int bloqueAnterior = 0;
            for (Metodo metodo : interfaz.metodos()) {
                int bloque = bloque(metodo);
                if (bloque < bloqueAnterior) {
                    violaciones.add(new Violacion(interfaz.ruta(), metodo.linea(),
                            "«" + metodo.nombre() + "» (" + NOMBRE_DE_BLOQUE.get(bloque) + ") está detrás de un método del bloque de "
                            + NOMBRE_DE_BLOQUE.get(bloqueAnterior) + "."));
                } else {
                    bloqueAnterior = bloque;
                }
            }
        }

        Violacion.assertNone("[I1] Una interfaz de servicio declara primero todas las acciones, después todas las validaciones"
                + " (validate*) y por último todos los allowProperties*; no se intercalan.", violaciones);
    }

    @Test
    @DisplayName("I2: las validaciones y los allowProperties de una interfaz de servicio van en el orden de sus acciones")
    void i2_validacionesYAllowPropertiesEnElOrdenDeLasAcciones() {
        List<Violacion> violaciones = new ArrayList<>();

        for (FuenteJava interfaz : INTERFACES) {
            Map<String, Integer> acciones = Orden.posiciones(List.of(), Orden.nombres(Optional.of(interfaz), metodo -> bloque(metodo) == 0));
            violaciones.addAll(Orden.enOrden(interfaz, interfaz.metodos().stream().filter(metodo -> bloque(metodo) == 1).toList(),
                    metodo -> posicionDeSuAccion(metodo, "validate", acciones), "el orden de las acciones"));
            violaciones.addAll(Orden.enOrden(interfaz, interfaz.metodos().stream().filter(metodo -> bloque(metodo) == 2).toList(),
                    metodo -> posicionDeSuAccion(metodo, "allowProperties", acciones), "el orden de las acciones"));
        }

        Violacion.assertNone("[I2] En una interfaz de servicio las validaciones (validate<Accion>) y los allowProperties<Accion>"
                + " van en el mismo orden en que están declaradas sus acciones.", violaciones);
    }

    @Test
    @DisplayName("I3: los bloques de una interfaz de servicio se separan con dos líneas en blanco y sin headers")
    void i3_losBloquesSeSeparanConDosLineasEnBlanco() {
        List<Violacion> violaciones = new ArrayList<>();

        for (FuenteJava interfaz : INTERFACES) {
            interfaz.headers().forEach(header -> violaciones.add(new Violacion(interfaz.ruta(), header.linea(),
                    "la interfaz lleva el header «" + header.titulo() + "»: sus bloques se separan solo con líneas en blanco.")));

            for (int i = 1; i < interfaz.metodos().size(); i++) {
                Metodo anterior = interfaz.metodos().get(i - 1);
                Metodo metodo = interfaz.metodos().get(i);
                if (bloque(metodo) == bloque(anterior)) {
                    continue;
                }
                int lineasEnBlanco = lineasEnBlancoEntre(interfaz, anterior, metodo);
                if (lineasEnBlanco != LINEAS_ENTRE_BLOQUES) {
                    violaciones.add(new Violacion(interfaz.ruta(), metodo.linea(),
                            "entre «" + anterior.nombre() + "» y «" + metodo.nombre() + "» empieza el bloque de "
                            + NOMBRE_DE_BLOQUE.get(bloque(metodo)) + " y hay " + lineasEnBlanco + " línea(s) en blanco en vez de " + LINEAS_ENTRE_BLOQUES + "."));
                }
            }
        }

        Violacion.assertNone("[I3] En una interfaz de servicio los bloques (acciones, validaciones, allowProperties) se separan"
                + " con exactamente dos líneas en blanco, sin comentarios de header.", violaciones);
    }

    private static final List<String> NOMBRE_DE_BLOQUE = List.of("acciones", "validaciones", "allowProperties");

    private static int bloque(Metodo metodo) {
        if (metodo.esAllowProperties()) {
            return 2;
        }

        return metodo.nombre().startsWith("validate") ? 1 : 0;
    }

    private static Optional<Integer> posicionDeSuAccion(Metodo metodo, String prefijo, Map<String, Integer> acciones) {
        String resto = metodo.nombre().substring(prefijo.length());
        if (resto.isEmpty()) {
            return Optional.empty();
        }

        return Optional.ofNullable(acciones.get(Character.toLowerCase(resto.charAt(0)) + resto.substring(1)));
    }

    /** Las líneas en blanco que hay entre dos métodos seguidos, sin contar el Javadoc o comentario pegado al segundo. */
    private static int lineasEnBlancoEntre(FuenteJava interfaz, Metodo anterior, Metodo metodo) {
        long ultima = metodo.linea() - 1;
        while (ultima > anterior.lineaFin() && !interfaz.linea(ultima).isBlank()) {
            ultima--;
        }
        int lineasEnBlanco = 0;
        for (long linea = anterior.lineaFin() + 1; linea <= ultima; linea++) {
            if (interfaz.linea(linea).isBlank()) {
                lineasEnBlanco++;
            }
        }

        return lineasEnBlanco;
    }
}
