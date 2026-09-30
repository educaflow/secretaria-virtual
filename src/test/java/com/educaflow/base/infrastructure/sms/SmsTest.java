package com.educaflow.base.infrastructure.sms;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SmsTest {

    @Test
    void constructor_conValoresValidos_losGuarda() {
        Sms sms = new Sms("+34600000000", "Hola");

        assertEquals("+34600000000", sms.telefonoDestino());
        assertEquals("Hola", sms.mensaje());
    }

    @Test
    void constructor_conTelefonoDestinoNullOBlank_lanzaExcepcion() {
        assertThrows(NullPointerException.class, () -> new Sms(null, "Hola"));
        assertThrows(IllegalArgumentException.class, () -> new Sms("", "Hola"));
        assertThrows(IllegalArgumentException.class, () -> new Sms(" ", "Hola"));
    }

    @Test
    void constructor_conMensajeNullOBlank_lanzaExcepcion() {
        assertThrows(NullPointerException.class, () -> new Sms("+34600000000", null));
        assertThrows(IllegalArgumentException.class, () -> new Sms("+34600000000", ""));
        assertThrows(IllegalArgumentException.class, () -> new Sms("+34600000000", " "));
    }
}
