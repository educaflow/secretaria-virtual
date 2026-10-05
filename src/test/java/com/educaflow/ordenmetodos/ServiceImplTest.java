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
 * El orden de los métodos de las implementaciones de servicio ({@code *ServiceImpl.java}).
 *
 * <p>Una {@code *ServiceImpl} se organiza en cinco bloques, y dentro de ellos sigue el orden que
 * fija su interfaz ({@link InterfazDeServicioTest}):
 *
 * <ul>
 *   <li><b>S1</b>: los headers son {@code Métodos de Validación}, {@code AllowProperties},
 *       {@code Action Rules} y {@code Otras funciones}, en ese orden, sin repetirse, cada uno con
 *       tres líneas de la misma longitud.
 *       Están <b>siempre todos</b>, aunque su bloque no tenga métodos: sirven de plantilla.</li>
 *   <li><b>S2</b>: cada método está en su bloque. Antes del primer header, las acciones (públicas);
 *       en {@code Métodos de Validación}, los {@code validate*} y sus auxiliares privados; en
 *       {@code AllowProperties}, los {@code allowProperties*} y sus auxiliares privados; en
 *       {@code Action Rules}, los {@code fireActionRule_*} privados; en {@code Otras funciones}, el
 *       resto de privados.</li>
 *   <li><b>S3</b>: dentro de los bloques de acciones, validaciones y {@code allowProperties}, los
 *       métodos van en el orden de la interfaz, con los heredados de {@code ModelService}
 *       ({@code insert}, {@code update}, {@code remove} y los suyos) delante.</li>
 * </ul>
 *
 * <p>La regla en prosa está en el skill {@code k-sistemas} ({@code servicios.md}), que debe
 * mantenerse coherente con estos tests.
 *
 * <p><b>Estos tests se escriben A MANO.</b> Este fichero es la fuente de verdad y se edita
 * directamente.
 */
class ServiceImplTest {

    private static final String VALIDACION = "Métodos de Validación";
    private static final String ALLOW_PROPERTIES = "AllowProperties";
    private static final String ACTION_RULES = "Action Rules";
    private static final String OTRAS = "Otras funciones";
    private static final List<String> HEADERS = List.of(VALIDACION, ALLOW_PROPERTIES, ACTION_RULES, OTRAS);

    private static final List<FuenteJava> IMPLEMENTACIONES = FuenteJava.conSufijo("ServiceImpl.java");

    @Test
    @DisplayName("S1: los headers de una *ServiceImpl son los cuatro previstos, todos, en orden y bien formados")
    void s1_losHeadersSonLosPrevistos() {
        List<Violacion> violaciones = new ArrayList<>();

        for (FuenteJava implementacion : IMPLEMENTACIONES) {
            violaciones.addAll(Orden.headers(implementacion, HEADERS));
        }

        Violacion.assertNone("[S1] Los headers de una *ServiceImpl son " + HEADERS + ", en ese orden y sin repetirse; cada uno son"
                + " tres líneas /*****…*****/ de la misma longitud con el título en la del medio. Están siempre todos, aunque su bloque esté vacío.", violaciones);
    }

    @Test
    @DisplayName("S2: cada método de una *ServiceImpl está en su bloque")
    void s2_cadaMetodoEstaEnSuBloque() {
        List<Violacion> violaciones = new ArrayList<>();

        for (FuenteJava implementacion : IMPLEMENTACIONES) {
            for (Metodo metodo : implementacion.metodos()) {
                String bloque = implementacion.bloqueDe(metodo).orElse("");
                fueraDeSitio(metodo, bloque).ifPresent(motivo -> violaciones.add(new Violacion(implementacion.ruta(), metodo.linea(),
                        "«" + metodo.nombre() + "» está en " + (bloque.isEmpty() ? "el bloque de acciones (antes del primer header)" : "«" + bloque + "»")
                        + ": " + motivo)));
            }
        }

        Violacion.assertNone("[S2] En una *ServiceImpl cada método está en su bloque: las acciones públicas antes del primer header;"
                + " los validate* (y sus auxiliares privados) en «" + VALIDACION + "»; los allowProperties* en «" + ALLOW_PROPERTIES
                + "»; los fireActionRule_* privados en «" + ACTION_RULES + "»; y el resto de privados en «" + OTRAS + "».", violaciones);
    }

    @Test
    @DisplayName("S3: dentro de cada bloque, los métodos de una *ServiceImpl siguen el orden de su interfaz")
    void s3_losMetodosSiguenElOrdenDeLaInterfaz() {
        List<Violacion> violaciones = new ArrayList<>();

        for (FuenteJava implementacion : IMPLEMENTACIONES) {
            Optional<FuenteJava> interfaz = implementacion.interfazDelServicio("ServiceImpl.java");
            Map<String, Integer> acciones = Orden.posiciones(List.of("insert", "update", "remove"),
                    Orden.nombres(interfaz, metodo -> !metodo.esValidacion() && !metodo.esAllowProperties()));
            Map<String, Integer> validaciones = Orden.posiciones(List.of("validate", "validateInsert", "validateUpdate", "validateRemove"),
                    Orden.nombres(interfaz, metodo -> metodo.nombre().startsWith("validate")));
            Map<String, Integer> allowProperties = Orden.posiciones(
                    List.of("allowPropertiesInsert", "allowPropertiesUpdate", "allowPropertiesRemove"),
                    Orden.nombres(interfaz, Metodo::esAllowProperties));

            List<Metodo> accesibles = implementacion.metodos().stream().filter(Metodo::accesible).toList();
            violaciones.addAll(Orden.enOrden(implementacion,
                    accesibles.stream().filter(metodo -> !metodo.esValidacion() && !metodo.esAllowProperties()).toList(),
                    metodo -> Optional.ofNullable(acciones.get(metodo.nombre())), "el orden de las acciones de la interfaz"));
            violaciones.addAll(Orden.enOrden(implementacion,
                    accesibles.stream().filter(Metodo::esValidacion).toList(),
                    metodo -> Optional.ofNullable(validaciones.get(metodo.nombre())), "el orden de las validaciones de la interfaz"));
            violaciones.addAll(Orden.enOrden(implementacion,
                    accesibles.stream().filter(Metodo::esAllowProperties).toList(),
                    metodo -> Optional.ofNullable(allowProperties.get(metodo.nombre())), "el orden de los allowProperties de la interfaz"));
        }

        Violacion.assertNone("[S3] Dentro de los bloques de acciones, validaciones y allowProperties, los métodos de una *ServiceImpl"
                + " van en el mismo orden en que los declara su interfaz, con los heredados de ModelService (insert, update, remove y"
                + " sus validate*/allowProperties*) delante.", violaciones);
    }

    /** Por qué el método no puede estar en ese bloque, o vacío si está bien colocado. */
    private static Optional<String> fueraDeSitio(Metodo metodo, String bloque) {
        return switch (bloque) {
            case "" -> !metodo.accesible() ? Optional.of("es privado; va en el bloque con header que le corresponda.")
                    : metodo.esAllowProperties() ? Optional.of("va en «" + ALLOW_PROPERTIES + "».")
                    : metodo.esValidacion() ? Optional.of("va en «" + VALIDACION + "».")
                    : Optional.empty();
            case VALIDACION -> metodo.esActionRule() ? Optional.of("va en «" + ACTION_RULES + "».")
                    : metodo.esAllowProperties() ? Optional.of("va en «" + ALLOW_PROPERTIES + "».")
                    : metodo.accesible() && !metodo.esValidacion() ? Optional.of("es una acción; va antes del primer header.")
                    : Optional.empty();
            case ALLOW_PROPERTIES -> metodo.esActionRule() ? Optional.of("va en «" + ACTION_RULES + "».")
                    : metodo.esAllowProperties() ? Optional.empty()
                    : metodo.esValidacion() ? Optional.of("va en «" + VALIDACION + "».")
                    : metodo.accesible() ? Optional.of("es una acción; va antes del primer header.")
                    : Optional.empty();
            case ACTION_RULES -> metodo.esActionRule() && !metodo.accesible() ? Optional.empty()
                    : Optional.of("ahí solo van los fireActionRule_* privados.");
            case OTRAS -> metodo.accesible() ? Optional.of("es público; ahí solo van métodos privados.")
                    : metodo.esActionRule() ? Optional.of("va en «" + ACTION_RULES + "».")
                    : metodo.esValidacion() ? Optional.of("va en «" + VALIDACION + "».")
                    : metodo.esAllowProperties() ? Optional.of("va en «" + ALLOW_PROPERTIES + "».")
                    : Optional.empty();
            default -> Optional.empty();
        };
    }
}
