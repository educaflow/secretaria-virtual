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
 * El orden de los métodos de los controladores ({@code *Controller.java}).
 *
 * <p>Un controlador se organiza en tres bloques, y dentro de ellos sigue el orden que fija la
 * interfaz de su servicio ({@link InterfazDeServicioTest}):
 *
 * <ul>
 *   <li><b>C1</b>: los headers son {@code Acciones de Validaciones} y {@code Métodos privados}, en
 *       ese orden, sin repetirse, cada uno con tres líneas de la misma longitud.
 *       Están <b>siempre todos</b>, aunque su bloque no tenga métodos: sirven de plantilla.</li>
 *   <li><b>C2</b>: cada método está en su bloque. Antes del primer header, las acciones (públicas
 *       que no son {@code validate*}); en {@code Acciones de Validaciones}, los {@code validate*}
 *       públicos; en {@code Métodos privados}, los que no son públicos.</li>
 *   <li><b>C3</b>: las acciones van en el orden de los métodos de la interfaz del servicio a los que
 *       llaman, y las validaciones igual. El método del servicio de una acción es el de su mismo
 *       nombre o, si no lo hay, el primero de la interfaz al que llama; una acción que no llama a
 *       ninguno no se compara.</li>
 * </ul>
 *
 * <p>La regla en prosa está en el skill {@code k-sistemas} ({@code controladores.md}), que debe
 * mantenerse coherente con estos tests.
 *
 * <p><b>Estos tests se escriben A MANO.</b> Este fichero es la fuente de verdad y se edita
 * directamente.
 */
class ControladorTest {

    private static final String VALIDACIONES = "Acciones de Validaciones";
    private static final String PRIVADOS = "Métodos privados";
    private static final List<String> HEADERS = List.of(VALIDACIONES, PRIVADOS);

    private static final List<FuenteJava> CONTROLADORES = FuenteJava.conSufijo("Controller.java");

    @Test
    @DisplayName("C1: los headers de un controlador son los dos previstos, todos, en orden y bien formados")
    void c1_losHeadersSonLosPrevistos() {
        List<Violacion> violaciones = new ArrayList<>();

        for (FuenteJava controlador : CONTROLADORES) {
            violaciones.addAll(Orden.headers(controlador, HEADERS));
        }

        Violacion.assertNone("[C1] Los headers de un controlador son " + HEADERS + ", en ese orden y sin repetirse; cada uno son"
                + " tres líneas /*****…*****/ de la misma longitud con el título en la del medio. Están siempre todos, aunque su bloque esté vacío.", violaciones);
    }

    @Test
    @DisplayName("C2: cada método de un controlador está en su bloque")
    void c2_cadaMetodoEstaEnSuBloque() {
        List<Violacion> violaciones = new ArrayList<>();

        for (FuenteJava controlador : CONTROLADORES) {
            for (Metodo metodo : controlador.metodos()) {
                String bloque = controlador.bloqueDe(metodo).orElse("");
                String suBloque = !metodo.accesible() ? PRIVADOS : esValidacion(metodo) ? VALIDACIONES : "";
                if (!bloque.equals(suBloque)) {
                    violaciones.add(new Violacion(controlador.ruta(), metodo.linea(),
                            "«" + metodo.nombre() + "» está en " + nombre(bloque) + " y va en " + nombre(suBloque) + "."));
                }
            }
        }

        Violacion.assertNone("[C2] En un controlador cada método está en su bloque: las acciones (públicas que no son validate*)"
                + " antes del primer header; los validate* públicos en «" + VALIDACIONES + "»; y los que no son públicos en «"
                + PRIVADOS + "».", violaciones);
    }

    @Test
    @DisplayName("C3: las acciones y las validaciones de un controlador siguen el orden de la interfaz de su servicio")
    void c3_losMetodosSiguenElOrdenDeLaInterfazDelServicio() {
        List<Violacion> violaciones = new ArrayList<>();

        for (FuenteJava controlador : CONTROLADORES) {
            Optional<FuenteJava> interfaz = controlador.interfazDelServicio("Controller.java");
            Map<String, Integer> acciones = Orden.posiciones(List.of("insert", "update", "remove"),
                    Orden.nombres(interfaz, metodo -> !metodo.nombre().startsWith("validate") && !metodo.esAllowProperties()));
            Map<String, Integer> validaciones = Orden.posiciones(List.of("validate", "validateInsert", "validateUpdate", "validateRemove"),
                    Orden.nombres(interfaz, metodo -> metodo.nombre().startsWith("validate")));

            List<Metodo> accesibles = controlador.metodos().stream().filter(Metodo::accesible).toList();
            violaciones.addAll(Orden.enOrden(controlador,
                    accesibles.stream().filter(metodo -> !esValidacion(metodo)).toList(),
                    metodo -> Orden.posicionPropiaODeLoQueLlama(metodo, acciones), "el orden de las acciones de la interfaz del servicio"));
            violaciones.addAll(Orden.enOrden(controlador,
                    accesibles.stream().filter(ControladorTest::esValidacion).toList(),
                    metodo -> Orden.posicionPropiaODeLoQueLlama(metodo, validaciones), "el orden de las validaciones de la interfaz del servicio"));
        }

        Violacion.assertNone("[C3] Las acciones de un controlador van en el mismo orden en que la interfaz de su servicio declara"
                + " los métodos a los que llaman, y sus validate* igual.", violaciones);
    }

    private static boolean esValidacion(Metodo metodo) {
        return metodo.nombre().startsWith("validate");
    }

    private static String nombre(String bloque) {
        return bloque.isEmpty() ? "el bloque de acciones (antes del primer header)" : "«" + bloque + "»";
    }
}
