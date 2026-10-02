package com.educaflow.base.infrastructure.fichero;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FicheroTest {

    private static final String FILE_NAME = "informe.pdf";
    private static final String MIME_TYPE = "application/pdf";

    private static Fichero fichero() {
        return new Fichero(FILE_NAME, new byte[]{1, 2, 3}, MIME_TYPE);
    }

    @Test
    void equals_mismaInstancia_true() {
        Fichero a = fichero();

        assertTrue(a.equals(a));
    }

    @Test
    void equals_null_false() {
        assertFalse(fichero().equals(null));
    }

    @Test
    void equals_objetoDeOtraClase_false() {
        assertFalse(fichero().equals("informe.pdf"));
    }

    @Test
    void equals_mismosValoresConArraysDistintos_trueYMismoHashCode() {
        Fichero a = fichero();
        Fichero b = fichero();

        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void equals_todosLosCamposNulos_true() {
        assertTrue(new Fichero(null, null, null).equals(new Fichero(null, null, null)));
    }

    @Test
    void equals_fileNameDistinto_false() {
        Fichero otro = new Fichero("otro.pdf", new byte[]{1, 2, 3}, MIME_TYPE);

        assertFalse(fichero().equals(otro));
    }

    @Test
    void equals_dataDistinto_false() {
        Fichero otro = new Fichero(FILE_NAME, new byte[]{1, 2, 4}, MIME_TYPE);

        assertFalse(fichero().equals(otro));
    }

    @Test
    void equals_mimeTypeDistinto_false() {
        Fichero otro = new Fichero(FILE_NAME, new byte[]{1, 2, 3}, "text/plain");

        assertFalse(fichero().equals(otro));
    }
}
