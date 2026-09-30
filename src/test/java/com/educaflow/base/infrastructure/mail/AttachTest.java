package com.educaflow.base.infrastructure.mail;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AttachTest {

    private static final String FILE_NAME = "informe.pdf";
    private static final String MIME_TYPE = "application/pdf";

    private static Attach attach() {
        return new Attach(FILE_NAME, new byte[]{1, 2, 3}, MIME_TYPE);
    }

    @Test
    void equals_mismaInstancia_true() {
        Attach a = attach();

        assertTrue(a.equals(a));
    }

    @Test
    void equals_null_false() {
        assertFalse(attach().equals(null));
    }

    @Test
    void equals_objetoDeOtraClase_false() {
        assertFalse(attach().equals("informe.pdf"));
    }

    @Test
    void equals_mismosValoresConArraysDistintos_trueYMismoHashCode() {
        Attach a = attach();
        Attach b = attach();

        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void equals_todosLosCamposNulos_true() {
        assertTrue(new Attach(null, null, null).equals(new Attach(null, null, null)));
    }

    @Test
    void equals_fileNameDistinto_false() {
        Attach otro = new Attach("otro.pdf", new byte[]{1, 2, 3}, MIME_TYPE);

        assertFalse(attach().equals(otro));
    }

    @Test
    void equals_dataDistinto_false() {
        Attach otro = new Attach(FILE_NAME, new byte[]{1, 2, 4}, MIME_TYPE);

        assertFalse(attach().equals(otro));
    }

    @Test
    void equals_mimeTypeDistinto_false() {
        Attach otro = new Attach(FILE_NAME, new byte[]{1, 2, 3}, "text/plain");

        assertFalse(attach().equals(otro));
    }
}
