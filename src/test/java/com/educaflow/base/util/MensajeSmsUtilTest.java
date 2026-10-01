package com.educaflow.base.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MensajeSmsUtilTest {

    private static final String MENSAJE_OBLIGATORIO = "El mensaje del SMS es obligatorio";
    private static final String TODOS_LOS_EXTENDIDOS = "\f^{}\\[~]|€";

    @Test
    void cabeEnUnSms_textoCortoBasico_devuelveTrue() {
        assertTrue(MensajeSmsUtil.cabeEnUnSms("Mañana no hay clase"));
    }

    @Test
    void cabeEnUnSms_cientoSesentaCaracteresBasicos_devuelveTrue() {
        assertTrue(MensajeSmsUtil.cabeEnUnSms("a".repeat(160)));
    }

    @Test
    void cabeEnUnSms_cientoSesentaYUnCaracteresBasicos_devuelveFalse() {
        assertFalse(MensajeSmsUtil.cabeEnUnSms("a".repeat(161)));
    }

    @Test
    void cabeEnUnSms_caracterExtendidoCuentaDosUnidades_devuelveTrueEnElLimite() {
        assertTrue(MensajeSmsUtil.cabeEnUnSms("a".repeat(158) + "€"));
    }

    @Test
    void cabeEnUnSms_caracterExtendidoQueRebasaElLimite_devuelveFalse() {
        String mensaje = "a".repeat(159) + "€";

        assertEquals(160, mensaje.length());
        assertFalse(MensajeSmsUtil.cabeEnUnSms(mensaje));
    }

    @Test
    void cabeEnUnSms_todosLosCaracteresExtendidos_cuentanDosUnidadesCadaUno() {
        String ochenta = TODOS_LOS_EXTENDIDOS.repeat(8);
        String ochentaYUno = ochenta + TODOS_LOS_EXTENDIDOS.charAt(0);

        assertEquals(80, ochenta.length());
        assertEquals(81, ochentaYUno.length());
        assertTrue(MensajeSmsUtil.cabeEnUnSms(ochenta));
        assertFalse(MensajeSmsUtil.cabeEnUnSms(ochentaYUno));
    }

    @Test
    void cabeEnUnSms_textoConAcentoNoGsm7DeSetentaCaracteres_devuelveTrue() {
        String mensaje = "ú" + "a".repeat(69);

        assertEquals(70, mensaje.length());
        assertTrue(MensajeSmsUtil.cabeEnUnSms(mensaje));
    }

    @Test
    void cabeEnUnSms_textoConAcentoNoGsm7DeSetentaYUnCaracteres_devuelveFalse() {
        String mensaje = "ú" + "a".repeat(70);

        assertEquals(71, mensaje.length());
        assertFalse(MensajeSmsUtil.cabeEnUnSms(mensaje));
    }

    @Test
    void cabeEnUnSms_unSoloCaracterFueraDeGsm7_cambiaElLimiteA70() {
        String basico = "a".repeat(100);
        String conAcento = "á" + "a".repeat(99);

        assertEquals(100, conAcento.length());
        assertTrue(MensajeSmsUtil.cabeEnUnSms(basico));
        assertFalse(MensajeSmsUtil.cabeEnUnSms(conAcento));
    }

    @Test
    void cabeEnUnSms_caracterFueraDelBmp_cuentaDosUnidadesUtf16() {
        String mensaje = "a".repeat(69) + "😀";

        assertEquals(71, mensaje.length());
        assertFalse(MensajeSmsUtil.cabeEnUnSms(mensaje));
    }

    @Test
    void cabeEnUnSms_mensajeNulo_lanzaNullPointerException() {
        NullPointerException excepcion = assertThrows(NullPointerException.class,
                () -> MensajeSmsUtil.cabeEnUnSms(null));

        assertEquals(MENSAJE_OBLIGATORIO, excepcion.getMessage());
    }

    @Test
    void cabeEnUnSms_mensajeVacioOEnBlanco_lanzaIllegalArgumentException() {
        IllegalArgumentException vacio = assertThrows(IllegalArgumentException.class,
                () -> MensajeSmsUtil.cabeEnUnSms(""));
        IllegalArgumentException enBlanco = assertThrows(IllegalArgumentException.class,
                () -> MensajeSmsUtil.cabeEnUnSms("   "));

        assertEquals(MENSAJE_OBLIGATORIO, vacio.getMessage());
        assertEquals(MENSAJE_OBLIGATORIO, enBlanco.getMessage());
    }
}
