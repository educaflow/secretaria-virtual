package com.educaflow.subsystem.registrousuario.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class DatosBasicosUsuarioTest {

    private static final String PASSWORD = "12345678";

    @Test
    void creaElRegistroConDatosValidos() {
        DatosBasicosUsuario datos = new DatosBasicosUsuario("Ana", "Pérez", PASSWORD, PASSWORD, "es");

        assertEquals("Ana", datos.nombre());
        assertEquals("Pérez", datos.apellidos());
        assertEquals(PASSWORD, datos.password());
        assertEquals(PASSWORD, datos.passwordRepeat());
        assertEquals("es", datos.idioma());
    }

    @Test
    void nombreNullLanzaNullPointerException() {
        NullPointerException ex = assertThrows(NullPointerException.class,
                () -> new DatosBasicosUsuario(null, "Pérez", PASSWORD, PASSWORD, "es"));
        assertEquals("nombre no puede ser null ni blank", ex.getMessage());
    }

    @Test
    void apellidosNullLanzaNullPointerException() {
        NullPointerException ex = assertThrows(NullPointerException.class,
                () -> new DatosBasicosUsuario("Ana", null, PASSWORD, PASSWORD, "es"));
        assertEquals("apellidos no puede ser null ni blank", ex.getMessage());
    }

    @Test
    void passwordNullLanzaNullPointerException() {
        NullPointerException ex = assertThrows(NullPointerException.class,
                () -> new DatosBasicosUsuario("Ana", "Pérez", null, PASSWORD, "es"));
        assertEquals("password no puede ser null ni blank", ex.getMessage());
    }

    @Test
    void passwordRepeatNullLanzaNullPointerException() {
        NullPointerException ex = assertThrows(NullPointerException.class,
                () -> new DatosBasicosUsuario("Ana", "Pérez", PASSWORD, null, "es"));
        assertEquals("passwordRepeat no puede ser null ni blank", ex.getMessage());
    }

    @Test
    void idiomaNullLanzaNullPointerException() {
        NullPointerException ex = assertThrows(NullPointerException.class,
                () -> new DatosBasicosUsuario("Ana", "Pérez", PASSWORD, PASSWORD, null));
        assertEquals("idioma no puede ser null ni blank", ex.getMessage());
    }

    @Test
    void nombreBlankLanzaIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new DatosBasicosUsuario("  ", "Pérez", PASSWORD, PASSWORD, "es"));
        assertEquals("nombre no puede ser null ni blank", ex.getMessage());
    }

    @Test
    void apellidosBlankLanzaIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new DatosBasicosUsuario("Ana", "", PASSWORD, PASSWORD, "es"));
        assertEquals("apellidos no puede ser null ni blank", ex.getMessage());
    }

    @Test
    void passwordBlankLanzaIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new DatosBasicosUsuario("Ana", "Pérez", " ", PASSWORD, "es"));
        assertEquals("password no puede ser null ni blank", ex.getMessage());
    }

    @Test
    void passwordRepeatBlankLanzaIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new DatosBasicosUsuario("Ana", "Pérez", PASSWORD, "\t", "es"));
        assertEquals("passwordRepeat no puede ser null ni blank", ex.getMessage());
    }

    @Test
    void idiomaBlankLanzaIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new DatosBasicosUsuario("Ana", "Pérez", PASSWORD, PASSWORD, ""));
        assertEquals("idioma no puede ser null ni blank", ex.getMessage());
    }

    @Test
    void passwordsDistintasLanzaIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new DatosBasicosUsuario("Ana", "Pérez", PASSWORD, "87654321", "es"));
        assertEquals("password y passwordRepeat deben ser iguales", ex.getMessage());
    }

    @Test
    void passwordCortaLanzaIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new DatosBasicosUsuario("Ana", "Pérez", "1234567", "1234567", "es"));
        assertEquals("password debe tener al menos 8 caracteres", ex.getMessage());
    }

    @Test
    void passwordDeExactamenteOchoCaracteresEsValida() {
        DatosBasicosUsuario datos = new DatosBasicosUsuario("Ana", "Pérez", "abcdefgh", "abcdefgh", "es");
        assertEquals("abcdefgh", datos.password());
    }
}
