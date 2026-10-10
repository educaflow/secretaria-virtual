package com.educaflow.base.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NumeroTelefonoTest {

    /*************************************** Constructor ***************************************/

    @Test
    void constructor_telefonoNoParseable_noLanza() {
        assertDoesNotThrow(() -> new NumeroTelefono("no-es-un-telefono"));
    }

    @Test
    void constructor_telefonoNulo_noLanza() {
        assertDoesNotThrow(() -> new NumeroTelefono(null));
    }

    /*************************************** esValidoDeEspana ***************************************/

    @Test
    void esValidoDeEspana_movilEspanol_devuelveTrue() {
        assertTrue(new NumeroTelefono("600111222").esValidoDeEspana());
    }

    @Test
    void esValidoDeEspana_fijoEspanol_devuelveTrue() {
        assertTrue(new NumeroTelefono("963000000").esValidoDeEspana());
    }

    @Test
    void esValidoDeEspana_fijoFrancesValido_devuelveFalse() {
        assertFalse(new NumeroTelefono("+33145678901").esValidoDeEspana());
    }

    @Test
    void esValidoDeEspana_numeroIncompleto_devuelveFalse() {
        assertFalse(new NumeroTelefono("96300").esValidoDeEspana());
    }

    @Test
    void esValidoDeEspana_nuloOTextoNoParseable_devuelveFalseSinLanzar() {
        assertFalse(assertDoesNotThrow(() -> new NumeroTelefono(null).esValidoDeEspana()));
        assertFalse(assertDoesNotThrow(() -> new NumeroTelefono("no-es-un-telefono").esValidoDeEspana()));
    }

    /*************************************** esMovilDeEspana ***************************************/

    @Test
    void esMovilDeEspana_movilEspanolSinPrefijo_devuelveTrue() {
        assertTrue(new NumeroTelefono("600111222").esMovilDeEspana());
    }

    @Test
    void esMovilDeEspana_movilEspanolEnE164_devuelveTrue() {
        assertTrue(new NumeroTelefono("+34600111222").esMovilDeEspana());
    }

    @Test
    void esMovilDeEspana_movilEspanolConEspacios_devuelveTrue() {
        assertTrue(new NumeroTelefono("600 11 12 22").esMovilDeEspana());
    }

    @Test
    void esMovilDeEspana_fijoEspanol_devuelveFalse() {
        assertFalse(new NumeroTelefono("963000000").esMovilDeEspana());
    }

    @Test
    void esMovilDeEspana_movilFrancesValido_devuelveFalse() {
        assertFalse(new NumeroTelefono("+33612345678").esMovilDeEspana());
    }

    @Test
    void esMovilDeEspana_numeroIncompleto_devuelveFalse() {
        assertFalse(new NumeroTelefono("60011").esMovilDeEspana());
    }

    @Test
    void esMovilDeEspana_textoNoParseable_devuelveFalseSinLanzar() {
        boolean resultado = assertDoesNotThrow(() -> new NumeroTelefono("no-es-un-telefono").esMovilDeEspana());

        assertFalse(resultado);
    }

    @Test
    void esMovilDeEspana_nuloVacioOBlanco_devuelveFalseSinLanzar() {
        assertFalse(assertDoesNotThrow(() -> new NumeroTelefono(null).esMovilDeEspana()));
        assertFalse(assertDoesNotThrow(() -> new NumeroTelefono("").esMovilDeEspana()));
        assertFalse(assertDoesNotThrow(() -> new NumeroTelefono("   ").esMovilDeEspana()));
    }

    /*************************************** enFormatoE164 ***************************************/

    @Test
    void enFormatoE164_movilEspanolSinPrefijo_devuelveElNumeroConPrefijo34() {
        assertEquals("+34600111222", new NumeroTelefono("600111222").enFormatoE164());
    }

    @Test
    void enFormatoE164_movilYaEnE164_devuelveElMismoNumero() {
        assertEquals("+34600111222", new NumeroTelefono("+34600111222").enFormatoE164());
    }

    @Test
    void enFormatoE164_movilConEspacios_devuelveElNumeroCanonico() {
        assertEquals("+34600111222", new NumeroTelefono("600 11 12 22").enFormatoE164());
    }

    @Test
    void enFormatoE164_fijoEspanol_lanzaIllegalStateException() {
        NumeroTelefono fijo = new NumeroTelefono("963000000");

        assertThrows(IllegalStateException.class, fijo::enFormatoE164);
    }

    @Test
    void enFormatoE164_movilFrancesValido_lanzaIllegalStateException() {
        NumeroTelefono movilFrances = new NumeroTelefono("+33612345678");

        assertThrows(IllegalStateException.class, movilFrances::enFormatoE164);
    }

    @Test
    void enFormatoE164_telefonoNoParseableONulo_lanzaIllegalStateException() {
        NumeroTelefono nulo = new NumeroTelefono(null);
        NumeroTelefono noParseable = new NumeroTelefono("no-es-un-telefono");

        assertThrows(IllegalStateException.class, nulo::enFormatoE164);
        assertThrows(IllegalStateException.class, noParseable::enFormatoE164);
    }
}
