package com.educaflow.base.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExceptionUtilTest {

    @Test
    void getTraceAsString_excepcionConMensaje_devuelveElNombreDeLaClaseElMensajeYLaTraza() {
        RuntimeException excepcion = assertThrows(RuntimeException.class, () -> {
            throw new RuntimeException("Twilio caído");
        });

        String traza = ExceptionUtil.getTraceAsString(excepcion);

        assertTrue(traza.contains("java.lang.RuntimeException"));
        assertTrue(traza.contains("Twilio caído"));
        assertTrue(traza.contains("\tat "));
    }

    @Test
    void getTraceAsString_excepcionConCausa_incluyeLaCausaEncadenada() {
        RuntimeException excepcion = new RuntimeException("Fallo al enviar SMS", new IllegalStateException("socket cerrado"));

        String traza = ExceptionUtil.getTraceAsString(excepcion);

        assertTrue(traza.contains("Fallo al enviar SMS"));
        assertTrue(traza.contains("Caused by"));
        assertTrue(traza.contains("socket cerrado"));
    }

    @Test
    void getTraceAsString_excepcionSinMensaje_devuelveAlMenosLaClaseYLaTraza() {
        RuntimeException excepcion = new RuntimeException();

        String traza = ExceptionUtil.getTraceAsString(excepcion);

        assertNotNull(traza);
        assertFalse(traza.isBlank());
        assertTrue(traza.contains("java.lang.RuntimeException"));
    }
}
