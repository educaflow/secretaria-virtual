package com.educaflow.subsystem.tramitador.tramitacion.core;

import com.educaflow.base.infrastructure.validation.engine.BeanValidationRules;
import com.educaflow.base.infrastructure.validation.engine.FieldValidationRules;
import com.educaflow.base.infrastructure.validation.engine.ValidationRule;
import com.educaflow.subsystem.common.db.Persona;
import com.educaflow.subsystem.tramitador.tramitacion.validation.BeanValidationRulesForStateAndEvent;
import com.educaflow.subsystem.tramitador.tramitacion.validation.StateEventValidator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import kotlin.reflect.KFunction;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TramitadorTest {

    @Nested
    class ExigeMismaPersona {

        @Test
        void ambasNulas_noLanza() {
            assertDoesNotThrow(() -> exigeMismaPersona(null, null, "personaSolicitante"));
        }

        @Test
        void mismaId_noLanza() {
            assertDoesNotThrow(() -> exigeMismaPersona(persona(5L), persona(5L), "personaSolicitante"));
        }

        @Test
        void ambasSinId_noLanza() {
            assertDoesNotThrow(() -> exigeMismaPersona(persona(null), persona(null), "personaInteresada"));
        }

        @Test
        void idDistinta_lanzaConIdsYCampo() {
            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> exigeMismaPersona(persona(2L), persona(1L), "personaInteresada"));

            assertEquals("La petición intenta cambiar la persona del campo 'personaInteresada' del expediente: de 1 a 2.", ex.getMessage());
        }

        @Test
        void personaNulaYOriginalSinId_lanzaAunqueLasIdsCoincidanEnNull() {
            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> exigeMismaPersona(null, persona(null), "personaSolicitante"));

            assertEquals("La petición intenta cambiar la persona del campo 'personaSolicitante' del expediente: de null a null.", ex.getMessage());
        }

        @Test
        void personaConIdYOriginalNula_lanza() {
            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> exigeMismaPersona(persona(7L), null, "personaSolicitante"));

            assertEquals("La petición intenta cambiar la persona del campo 'personaSolicitante' del expediente: de null a 7.", ex.getMessage());
        }

        @Test
        void personaNulaYOriginalConId_lanza() {
            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> exigeMismaPersona(null, persona(3L), "personaInteresada"));

            assertEquals("La petición intenta cambiar la persona del campo 'personaInteresada' del expediente: de 3 a null.", ex.getMessage());
        }
    }

    @Nested
    class GetBeansValidationRulesPorEstado {

        @Test
        void devuelveSoloLasReglasDeLosMetodosAnotadosDelEstado() throws Throwable {
            ValidatorConReglas validator = new ValidatorConReglas();

            List<BeanValidationRules> reglas = getBeansValidationRules(validator, "ENTRADA_DATOS");

            assertEquals(2, reglas.size());
            Set<BeanValidationRules> mismasInstancias = Collections.newSetFromMap(new IdentityHashMap<>());
            mismasInstancias.addAll(reglas);
            assertTrue(mismasInstancias.contains(validator.reglasPresentar));
            assertTrue(mismasInstancias.contains(validator.reglasGuardar));
        }

        @Test
        void metodoAnotadoQueDevuelveNull_lanzaEnvuelto() {
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> getBeansValidationRules(new ValidatorConNull(), "ENTRADA_DATOS"));

            assertEquals("Error al obtener las reglas de validación para el estado: ENTRADA_DATOS en " + ValidatorConNull.class.getName(), ex.getMessage());
            assertInstanceOf(RuntimeException.class, ex.getCause());
            assertEquals("El método retorno null:getForStateEntradaDatosInEventPresentar", ex.getCause().getMessage());
        }

        @Test
        void sinMetodosDelEstado_lanzaEnvuelto() {
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> getBeansValidationRules(new ValidatorConReglas(), "REVISION"));

            assertEquals("Error al obtener las reglas de validación para el estado: REVISION en " + ValidatorConReglas.class.getName(), ex.getMessage());
            assertEquals("No se han encontrado las reglas de validación para el estado: REVISION", ex.getCause().getMessage());
        }
    }

    @Nested
    class GetFieldsValidationRules {

        @Test
        void sinBeans_devuelveListaVacia() throws Throwable {
            assertTrue(getFieldsValidationRules(List.of(), "getNombre").isEmpty());
        }

        @Test
        void devuelveSoloLasSubreglasDeCampoDeLosCamposConEseMetodo() throws Throwable {
            FieldValidationRules subDireccion = campo("getCalle", List.of());
            FieldValidationRules subOtroBean = campo("getNumero", List.of());
            ValidationRule reglaSimple = mock(ValidationRule.class);

            FieldValidationRules direccion = campo("getDireccion", List.of(subDireccion, reglaSimple));
            FieldValidationRules otroCampo = campo("getNombre", List.of(campo("getApellido", List.of())));
            FieldValidationRules direccionOtroBean = campo("getDireccion", List.of(subOtroBean));

            List<BeanValidationRules> beans = List.of(
                    new BeanValidationRules(List.of(direccion, otroCampo)),
                    new BeanValidationRules(List.of()),
                    new BeanValidationRules(List.of(direccionOtroBean)));

            List<FieldValidationRules> resultado = getFieldsValidationRules(beans, "getDireccion");

            assertEquals(2, resultado.size());
            assertSame(subDireccion, resultado.get(0));
            assertSame(subOtroBean, resultado.get(1));
        }

        @Test
        void campoConEseMetodoSinSubreglasDeCampo_devuelveListaVacia() throws Throwable {
            FieldValidationRules nombre = campo("getNombre", List.of(mock(ValidationRule.class)));

            List<FieldValidationRules> resultado = getFieldsValidationRules(List.of(new BeanValidationRules(List.of(nombre))), "getNombre");

            assertTrue(resultado.isEmpty());
        }

        @Test
        void ningunCampoConEseMetodo_devuelveListaVacia() throws Throwable {
            FieldValidationRules nombre = campo("getNombre", List.of(campo("getApellido", List.of())));

            List<FieldValidationRules> resultado = getFieldsValidationRules(List.of(new BeanValidationRules(List.of(nombre))), "getDireccion");

            assertTrue(resultado.isEmpty());
        }
    }

    private static FieldValidationRules campo(String nombreMetodo, List<ValidationRule> reglas) {
        KFunction<?> metodo = mock(KFunction.class);
        when(metodo.getName()).thenReturn(nombreMetodo);
        return new FieldValidationRules(metodo, reglas);
    }

    @SuppressWarnings("unchecked")
    private static List<FieldValidationRules> getFieldsValidationRules(List<BeanValidationRules> beans, String methodName) throws Throwable {
        Method method = Tramitador.class.getDeclaredMethod("getFieldsValidationRules", List.class, String.class);
        method.setAccessible(true);
        try {
            return (List<FieldValidationRules>) method.invoke(new Tramitador(), beans, methodName);
        } catch (InvocationTargetException ex) {
            throw ex.getCause();
        }
    }

    public static class ValidatorConReglas implements StateEventValidator {
        final BeanValidationRules reglasPresentar = new BeanValidationRules(List.of());
        final BeanValidationRules reglasGuardar = new BeanValidationRules(List.of());
        final BeanValidationRules reglasSinAnotar = new BeanValidationRules(List.of());
        final BeanValidationRules reglasOtroEstado = new BeanValidationRules(List.of());

        @BeanValidationRulesForStateAndEvent
        public BeanValidationRules getForStateEntradaDatosInEventPresentar() {
            return reglasPresentar;
        }

        @BeanValidationRulesForStateAndEvent
        public BeanValidationRules getForStateEntradaDatosInEventGuardar() {
            return reglasGuardar;
        }

        public BeanValidationRules getForStateEntradaDatosInEventSinAnotar() {
            return reglasSinAnotar;
        }

        @BeanValidationRulesForStateAndEvent
        public BeanValidationRules getForStateFirmaInEventFirmar() {
            return reglasOtroEstado;
        }
    }

    public static class ValidatorConNull implements StateEventValidator {
        @BeanValidationRulesForStateAndEvent
        public BeanValidationRules getForStateEntradaDatosInEventPresentar() {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static List<BeanValidationRules> getBeansValidationRules(StateEventValidator validator, String state) throws Throwable {
        Method method = Tramitador.class.getDeclaredMethod("getBeansValidationRules", StateEventValidator.class, String.class);
        method.setAccessible(true);
        try {
            return (List<BeanValidationRules>) method.invoke(new Tramitador(), validator, state);
        } catch (InvocationTargetException ex) {
            throw ex.getCause();
        }
    }

    private static Persona persona(Long id) {
        Persona persona = new Persona();
        persona.setId(id);
        return persona;
    }

    private static void exigeMismaPersona(Persona persona, Persona personaOriginal, String nombreCampo) throws Throwable {
        Method method = Tramitador.class.getDeclaredMethod("exigeMismaPersona", Persona.class, Persona.class, String.class);
        method.setAccessible(true);
        try {
            method.invoke(null, persona, personaOriginal, nombreCampo);
        } catch (InvocationTargetException ex) {
            throw ex.getCause();
        }
    }
}
