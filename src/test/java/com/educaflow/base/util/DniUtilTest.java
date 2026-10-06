package com.educaflow.base.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DniUtilTest {

    @Test
    void isValid_conNullOLongitudDistintaDe9_devuelveFalse() {
        assertFalse(DniUtil.isValid(null));
        assertFalse(DniUtil.isValid("1234567Z"));
        assertFalse(DniUtil.isValid("0123456789Z"));
    }

    @Test
    void isValid_conCifSoloNumeros_compruebaElDigitoDeControl() {
        assertTrue(DniUtil.isValid("A58818501"));
        assertTrue(DniUtil.isValid("B12345674"));
        assertFalse(DniUtil.isValid("A58818502"));
    }

    @Test
    void isValid_conCifCuyoDigitoCalculadoEs10_loTrataComo0() {
        assertTrue(DniUtil.isValid("A00000000"));
        assertFalse(DniUtil.isValid("A00000001"));
    }

    @Test
    void isValid_conCifTerminadoEnLetra_compruebaLaLetraDeControl() {
        assertTrue(DniUtil.isValid("P1234567D"));
        assertTrue(DniUtil.isValid("Q2826000H"));
        assertFalse(DniUtil.isValid("P1234567E"));
    }

    @Test
    void isValid_conDni_compruebaLaLetraDeControl() {
        assertTrue(DniUtil.isValid("93882914L"));
        assertFalse(DniUtil.isValid("12345678A"));
    }

    @Test
    void isValid_conDnisDePruebaDeLaAeat_devuelveFalseAunqueLaLetraCuadre() {
        assertFalse(DniUtil.isValid("00000001R"));
        assertFalse(DniUtil.isValid("00000000T"));
        assertFalse(DniUtil.isValid("99999999R"));
    }

    @Test
    void isValid_conNie_sumaElPrefijoDeLaLetraInicial() {
        assertTrue(DniUtil.isValid("X1234567L"));
        assertTrue(DniUtil.isValid("Y1234567X"));
        assertTrue(DniUtil.isValid("Z1234567R"));
        assertFalse(DniUtil.isValid("X1234567X"));
    }

    @Test
    void isValid_conNieX0000000T_devuelveFalseAunqueLaLetraCuadre() {
        assertFalse(DniUtil.isValid("X0000000T"));
    }

    @Test
    void isValid_conNifEspecialKLM_compruebaRangoYLetraDeControl() {
        assertTrue(DniUtil.isValid("K0123456S"));
        assertTrue(DniUtil.isValid("L1234567L"));
        assertFalse(DniUtil.isValid("L1234567A"));
        assertFalse(DniUtil.isValid("K0012345A"));
        assertFalse(DniUtil.isValid("M5712345A"));
    }

    @Test
    void isValid_conFormatoNoReconocido_devuelveFalse() {
        assertFalse(DniUtil.isValid("ABCDEFGHI"));
        assertFalse(DniUtil.isValid("93882914l"));
    }
}
