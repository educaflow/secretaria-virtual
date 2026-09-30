package com.educaflow.subsystem.tramitador.tramitacion.internal;

import com.educaflow.subsystem.expedientes.db.Expediente;
import com.educaflow.subsystem.expedientes.db.TipoExpediente;
import com.google.inject.Injector;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class ExpedienteLocatorTest {

    private static final String FIXTURES = "com.educaflow.subsystem.tramitador.tramitacion.internal.fixtures";

    private final ExpedienteLocator expedienteLocator = new ExpedienteLocator(mock(Injector.class));

    private static TipoExpediente tipoExpediente(String basePackageName) {
        TipoExpediente tipoExpediente = new TipoExpediente();
        tipoExpediente.setCode("MI_TIPO");
        tipoExpediente.setName("Mi tipo");
        tipoExpediente.setBasePackageName(basePackageName);

        return tipoExpediente;
    }

    @Nested
    class GetModelClass {

        @Test
        void interfazParametrizadaConEntidad_devuelveLaEntidadSaltandoLasDemasInterfaces() {
            Class<? extends Expediente> modelClass = expedienteLocator.getModelClass(tipoExpediente(FIXTURES + ".conentidad"));

            assertSame(Expediente.class, modelClass);
        }

        @Test
        void implementadaEnCrudo_lanzaRuntimeExceptionConClaseYTipo() {
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> expedienteLocator.getModelClass(tipoExpediente(FIXTURES + ".encrudo")));

            assertTrue(ex.getMessage().startsWith("La clase " + FIXTURES + ".encrudo.InitialEventManagerImpl no declara"));
            assertTrue(ex.getMessage().contains("del tipo de expediente MI_TIPO."));
        }

        @Test
        void parametroDeTipoVariable_lanzaRuntimeException() {
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> expedienteLocator.getModelClass(tipoExpediente(FIXTURES + ".convariable")));

            assertTrue(ex.getMessage().startsWith("La clase " + FIXTURES + ".convariable.InitialEventManagerImpl no declara"));
        }

        @Test
        void paqueteSinInitialEventManagerImpl_lanzaRuntimeExceptionConClassNotFound() {
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> expedienteLocator.getModelClass(tipoExpediente(FIXTURES + ".noexiste")));

            assertEquals("No existe la clase " + FIXTURES + ".noexiste.InitialEventManagerImpl, que es la que"
                    + " debería atender el evento inicial del tipo de expediente MI_TIPO. Hay exactamente una"
                    + " por tipo de expediente, en la raíz de la carpeta de versión (el evento inicial no es"
                    + " de ninguna fase).", ex.getMessage());
            assertInstanceOf(ClassNotFoundException.class, ex.getCause());
        }

        @Test
        void sinBasePackageName_lanzaRuntimeException() {
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> expedienteLocator.getModelClass(tipoExpediente(" ")));

            assertTrue(ex.getMessage().startsWith("No existe el basePackageName para el tipo de expediente: Mi tipo."));
        }
    }
}
