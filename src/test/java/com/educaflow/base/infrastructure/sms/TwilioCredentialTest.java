package com.educaflow.base.infrastructure.sms;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TwilioCredentialTest {

    @Test
    void constructor_conValoresValidos_losGuarda() {
        TwilioCredential credential = new TwilioCredential("ACxxxx", "token");

        assertEquals("ACxxxx", credential.accountSid());
        assertEquals("token", credential.authToken());
    }

    @Test
    void constructor_conAccountSidNullOBlank_lanzaExcepcion() {
        assertThrows(NullPointerException.class, () -> new TwilioCredential(null, "token"));
        assertThrows(IllegalArgumentException.class, () -> new TwilioCredential("", "token"));
        assertThrows(IllegalArgumentException.class, () -> new TwilioCredential(" ", "token"));
    }

    @Test
    void constructor_conAuthTokenNullOBlank_lanzaExcepcion() {
        assertThrows(NullPointerException.class, () -> new TwilioCredential("ACxxxx", null));
        assertThrows(IllegalArgumentException.class, () -> new TwilioCredential("ACxxxx", ""));
        assertThrows(IllegalArgumentException.class, () -> new TwilioCredential("ACxxxx", " "));
    }

    @Test
    void toString_noMuestraElAuthToken() {
        assertFalse(new TwilioCredential("ACxxxx", "secreto").toString().contains("secreto"));
    }
}
