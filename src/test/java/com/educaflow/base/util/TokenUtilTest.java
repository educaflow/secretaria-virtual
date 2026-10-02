package com.educaflow.base.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenUtilTest {

    @Test
    void generateCodigoSeguro_Verificacion_devuelve26CaracteresConSuPropiaForma() {
        String codigo = TokenUtil.generateCodigoSeguroVerificacion();

        assertEquals(26, codigo.length());
        assertTrue(TokenUtil.isCodigoSeguroVerificacion(codigo));
    }

    @Test
    void generateCodigoVerificacionSeguro_noRepiteElCodigoVerificacion() {
        assertNotEquals(TokenUtil.generateCodigoSeguroVerificacion(), TokenUtil.generateCodigoSeguroVerificacion());
    }

    @Test
    void isCodigoSeguro_Verificacion_conNullOtraLongitudOCaracteresDeFueraDelAlfabeto_devuelveFalse() {
        assertFalse(TokenUtil.isCodigoSeguroVerificacion(null));
        assertFalse(TokenUtil.isCodigoSeguroVerificacion(""));
        assertFalse(TokenUtil.isCodigoSeguroVerificacion("0123456789ABCDEFGHJKMNPQR"));
        assertFalse(TokenUtil.isCodigoSeguroVerificacion("0123456789ABCDEFGHJKMNPQRST"));
        assertFalse(TokenUtil.isCodigoSeguroVerificacion("0123456789ABCDEFGHJKMNPQRI"));
        assertFalse(TokenUtil.isCodigoSeguroVerificacion("0123456789abcdefghjkmnpqrs"));
        assertFalse(TokenUtil.isCodigoSeguroVerificacion("../../../../etc/passwd0000"));
    }

    @Test
    void isCodigoSeguro_Verificacion_con26CaracteresDelAlfabeto_devuelveTrue() {
        assertTrue(TokenUtil.isCodigoSeguroVerificacion("0123456789ABCDEFGHJKMNPQRS"));
    }
}
