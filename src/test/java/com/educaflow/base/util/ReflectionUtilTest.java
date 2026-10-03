package com.educaflow.base.util;

import org.junit.jupiter.api.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReflectionUtilTest {

    @Retention(RetentionPolicy.RUNTIME)
    @interface Marca {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @interface OtraMarca {
    }

    /** Interfaz sin código: JaCoCo no le añade métodos sintéticos, así que solo declara {@code run}. */
    interface Solo {
        String run();
    }

    interface Proveedor<T> {
        T get();
    }

    /** Implementa una interfaz genérica: javac genera un método puente {@code Object get()} que copia la anotación. */
    static class ConPuente implements Proveedor<String> {
        @Marca
        @Override
        public String get() {
            return "x";
        }
    }

    static class Fixture {
        public String nombre() {
            return "";
        }

        public String nombre(int a) {
            return "" + a;
        }

        public String nombre(String s) {
            return s;
        }

        @Marca
        public Integer otro(int a, String b) {
            return a;
        }

        public void otro() {
        }
    }

    @Test
    void getMethod_porNombreYParametros_devuelveElMetodoExacto() throws Exception {
        Method method = ReflectionUtil.getMethod(Fixture.class, "nombre", null, null, new Class<?>[]{int.class}).orElseThrow();
        assertEquals(Fixture.class.getDeclaredMethod("nombre", int.class), method);
    }

    @Test
    void getMethod_conNombreQueNoExiste_devuelveVacio() {
        assertTrue(ReflectionUtil.getMethod(Fixture.class, "noExiste", null, null, null).isEmpty());
    }

    @Test
    void getMethod_conDistintoNumeroDeParametros_devuelveVacio() {
        assertTrue(ReflectionUtil.getMethod(Fixture.class, "otro", null, null, new Class<?>[]{int.class}).isEmpty());
    }

    @Test
    void getMethod_conMismoNumeroPeroDistintoTipoDeParametro_devuelveVacio() {
        assertTrue(ReflectionUtil.getMethod(Fixture.class, "nombre", null, null, new Class<?>[]{long.class}).isEmpty());
    }

    @Test
    void getMethod_conPrimerParametroIgualYSegundoDistinto_devuelveVacio() {
        assertTrue(ReflectionUtil.getMethod(Fixture.class, "otro", null, null, new Class<?>[]{int.class, Integer.class}).isEmpty());
    }

    @Test
    void getMethod_conVariosParametrosIguales_devuelveElMetodo() throws Exception {
        Method method = ReflectionUtil.getMethod(Fixture.class, "otro", null, null, new Class<?>[]{int.class, String.class}).orElseThrow();
        assertEquals(Fixture.class.getDeclaredMethod("otro", int.class, String.class), method);
    }

    @Test
    void getMethod_conRetornoNoAsignable_devuelveVacio() {
        assertTrue(ReflectionUtil.getMethod(Fixture.class, "nombre", Integer.class, null, new Class<?>[]{}).isEmpty());
    }

    @Test
    void getMethod_conRetornoSuperclaseDelDeclarado_loAcepta() throws Exception {
        Method method = ReflectionUtil.getMethod(Fixture.class, "nombre", CharSequence.class, null, new Class<?>[]{}).orElseThrow();
        assertEquals(Fixture.class.getDeclaredMethod("nombre"), method);
    }

    @Test
    void getMethod_soloPorAnotacion_devuelveElMetodoAnotado() throws Exception {
        Method method = ReflectionUtil.getMethod(Fixture.class, null, null, Marca.class, null).orElseThrow();
        assertEquals(Fixture.class.getDeclaredMethod("otro", int.class, String.class), method);
    }

    @Test
    void getMethod_conAnotacionQueNoTiene_devuelveVacio() {
        assertTrue(ReflectionUtil.getMethod(Fixture.class, "otro", null, OtraMarca.class, null).isEmpty());
    }

    @Test
    void getMethod_sinNingunFiltroEnClaseConUnSoloMetodo_loDevuelve() throws Exception {
        Method method = ReflectionUtil.getMethod(Solo.class, null, null, null, null).orElseThrow();
        assertEquals(Solo.class.getDeclaredMethod("run"), method);
    }

    @Test
    void getMethod_conVariosCandidatosSinFiltros_lanzaRuntimeExceptionConNA() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> ReflectionUtil.getMethod(Fixture.class, "nombre", null, null, null));
        assertEquals("Se encontró más de un método: nombre en la clase: " + Fixture.class.getName()
                + " con Nº parámetros: N/A y retorno: N/A y la anotación: N/A", ex.getMessage());
    }

    @Test
    void getMethod_conMetodoPuenteQueCoincideEnTodo_lanzaRuntimeExceptionConLosFiltros() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> ReflectionUtil.getMethod(ConPuente.class, "get", Object.class, Marca.class, new Class<?>[]{}));
        assertEquals("Se encontró más de un método: get en la clase: " + ConPuente.class.getName()
                + " con Nº parámetros: 0 y retorno: java.lang.Object y la anotación: " + Marca.class.getName(), ex.getMessage());
    }

    @Test
    void hasMethod_conMetodoExistente_devuelveTrue() {
        assertTrue(ReflectionUtil.hasMethod(Fixture.class, "nombre", null, null, new Class<?>[]{String.class}));
    }

    @Test
    void hasMethod_conMetodoInexistente_devuelveFalse() {
        assertFalse(ReflectionUtil.hasMethod(Fixture.class, "noExiste", null, null, null));
    }

    @Test
    void getMethod_devuelveMetodoInvocable() throws Exception {
        Method method = ReflectionUtil.getMethod(Fixture.class, "nombre", String.class, null, new Class<?>[]{String.class}).orElseThrow();
        assertNotNull(method);
        assertEquals("hola", method.invoke(new Fixture(), "hola"));
    }
}
