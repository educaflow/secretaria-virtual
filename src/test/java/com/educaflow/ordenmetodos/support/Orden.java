package com.educaflow.ordenmetodos.support;

import com.educaflow.ordenmetodos.support.FuenteJava.Header;
import com.educaflow.ordenmetodos.support.FuenteJava.Metodo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Las comprobaciones que comparten las reglas de orden de la interfaz, la {@code *ServiceImpl} y el
 * controlador.
 */
public final class Orden {

    private Orden() {
    }

    /**
     * La posición de cada método en la lista: primero los {@code heredados}, en ese orden, y detrás
     * los {@code propios}. Los nombres repetidos (sobrecargas) conservan la primera posición.
     */
    public static Map<String, Integer> posiciones(List<String> heredados, List<String> propios) {
        Map<String, Integer> posiciones = new LinkedHashMap<>();
        for (String nombre : heredados) {
            posiciones.putIfAbsent(nombre, posiciones.size());
        }
        for (String nombre : propios) {
            posiciones.putIfAbsent(nombre, posiciones.size());
        }

        return posiciones;
    }

    /** Los nombres de los métodos de la interfaz que cumplen el filtro, en su orden de declaración. */
    public static List<String> nombres(Optional<FuenteJava> interfaz, Predicate<Metodo> filtro) {
        return interfaz.map(fuente -> fuente.metodos().stream().filter(filtro).map(Metodo::nombre).toList()).orElse(List.of());
    }

    /**
     * Comprueba que los métodos están en orden no decreciente de la posición que les da
     * {@code posicion}. Los que no tienen posición no se comparan con nadie.
     */
    public static List<Violacion> enOrden(FuenteJava fuente, List<Metodo> metodos, Function<Metodo, Optional<Integer>> posicion,
            String queOrden) {
        List<Violacion> violaciones = new ArrayList<>();
        Metodo anterior = null;
        int posicionAnterior = -1;
        for (Metodo metodo : metodos) {
            Optional<Integer> actual = posicion.apply(metodo);
            if (actual.isEmpty()) {
                continue;
            }
            if (actual.get() < posicionAnterior) {
                violaciones.add(new Violacion(fuente.ruta(), metodo.linea(),
                        "«" + metodo.nombre() + "» está detrás de «" + anterior.nombre() + "», pero va delante según " + queOrden + "."));
            } else {
                anterior = metodo;
                posicionAnterior = actual.get();
            }
        }

        return violaciones;
    }

    /** La posición del propio método por su nombre o, si no la tiene, la de la primera de sus llamadas que la tenga. */
    public static Optional<Integer> posicionPropiaODeLoQueLlama(Metodo metodo, Map<String, Integer> posiciones) {
        if (posiciones.containsKey(metodo.nombre())) {
            return Optional.of(posiciones.get(metodo.nombre()));
        }

        return metodo.llamadas().stream().filter(posiciones::containsKey).map(posiciones::get).findFirst();
    }

    /**
     * Comprueba los headers de un fichero: cada uno son tres líneas de la misma longitud con el
     * título en la del medio, su título es uno de los {@code titulos}, aparecen en ese orden sin
     * repetirse y no falta ninguno (un bloque sin métodos también lleva su header, a modo de plantilla).
     */
    public static List<Violacion> headers(FuenteJava fuente, List<String> titulos) {
        List<Violacion> violaciones = new ArrayList<>();
        int indiceAnterior = -1;
        for (Header header : fuente.headers()) {
            if (!header.bienFormado()) {
                violaciones.add(new Violacion(fuente.ruta(), header.linea(),
                        "el header «" + header.titulo() + "» no son tres líneas /*****…*****/ de la misma longitud con el título en la del medio."));
            }
            int indice = titulos.indexOf(header.titulo());
            if (indice < 0) {
                violaciones.add(new Violacion(fuente.ruta(), header.linea(),
                        "el header «" + header.titulo() + "» no es ninguno de los permitidos: " + titulos + "."));
                continue;
            }
            if (indice <= indiceAnterior) {
                violaciones.add(new Violacion(fuente.ruta(), header.linea(),
                        "el header «" + header.titulo() + "» está repetido o fuera de orden; el orden es " + titulos + "."));
            }
            indiceAnterior = indice;

        }
        for (String titulo : titulos) {
            if (fuente.headers().stream().noneMatch(header -> header.titulo().equals(titulo))) {
                violaciones.add(new Violacion(fuente.ruta(), 1,
                        "falta el header «" + titulo + "»: se pone siempre, aunque su bloque no tenga métodos, para que sirva de plantilla."));
            }
        }

        return violaciones;
    }
}
