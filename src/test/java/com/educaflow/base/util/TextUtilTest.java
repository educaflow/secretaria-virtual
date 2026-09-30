package com.educaflow.base.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TextUtilTest {

    @Test
    void requireNonBlank_conTexto_devuelveElMismoTexto() {
        assertEquals("abc", TextUtil.requireNonBlank("abc", "mensaje"));
    }

    @Test
    void requireNonBlank_conNull_lanzaNullPointerException() {
        NullPointerException ex = assertThrows(NullPointerException.class, () -> TextUtil.requireNonBlank(null, "mensaje"));
        assertEquals("mensaje", ex.getMessage());
    }

    @Test
    void requireNonBlank_conVacioOSoloEspacios_lanzaIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> TextUtil.requireNonBlank("", "mensaje"));
        assertEquals("mensaje", ex.getMessage());
        assertThrows(IllegalArgumentException.class, () -> TextUtil.requireNonBlank("   ", "mensaje"));
    }
    @Test
    void sanitizeFileName_conNullOVacioOSoloEspacios_lanzaRuntimeException() {
        RuntimeException ex = assertThrows(RuntimeException.class, () -> TextUtil.sanitizeFileName(null));
        assertEquals("El nombre de archivo no puede ser nulo o vacío", ex.getMessage());
        assertThrows(RuntimeException.class, () -> TextUtil.sanitizeFileName(""));
        assertThrows(RuntimeException.class, () -> TextUtil.sanitizeFileName("   "));
    }

    @Test
    void sanitizeFileName_quitaAcentosYCambiaSeparadoresYEspaciosPorGuionBajo() {
        assertEquals("cancion_de_Nino_a_b_c.pdf", TextUtil.sanitizeFileName("canción de Niño a/b\\c.pdf"));
    }

    @Test
    void sanitizeFileName_eliminaCaracteresPeligrososYNoPermitidos() {
        assertEquals("abc.txt", TextUtil.sanitizeFileName("a:b*?\"<>|c€#.txt"));
    }

    @Test
    void sanitizeFileName_siNoQuedaNadaDevuelveFile() {
        assertEquals("file", TextUtil.sanitizeFileName("€#@"));
    }

    @Test
    void sanitizeFileName_conNombreReservadoDeWindowsAnhadePrefijo() {
        assertEquals("_con", TextUtil.sanitizeFileName("con"));
        assertEquals("_LPT1", TextUtil.sanitizeFileName("LPT1"));
        assertEquals("con.txt", TextUtil.sanitizeFileName("con.txt"));
    }

    @Test
    void sanitizeFileName_truncaA255Caracteres() {
        String largo = "a".repeat(300);
        assertEquals("a".repeat(255), TextUtil.sanitizeFileName(largo));
        String justo = "b".repeat(255);
        assertEquals(justo, TextUtil.sanitizeFileName(justo));
    }
}
