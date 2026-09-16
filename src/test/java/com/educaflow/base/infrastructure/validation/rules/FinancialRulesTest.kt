package com.educaflow.base.infrastructure.validation.rules

import com.axelor.db.modelservice.BusinessMessages
import com.axelor.i18n.I18n
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.quality.Strictness

/**
 * Tests de [Iban]. Se comprueba qué valores acepta y cuáles rechaza, nunca el texto concreto del mensaje:
 * ese literal puede cambiar sin que cambie la regla. `I18n` se mockea porque sin contexto de Axelor
 * arrancado la traducción no está disponible.
 *
 * El IBAN válido usado (`ES9121000418450200051332`) es el ejemplo público de referencia del formato IBAN
 * español, no un dato real.
 */
class FinancialRulesTest {

    private var i18n: MockedStatic<I18n>? = null

    /** Segundo parámetro de `validate`: esta regla no lo usa, solo mira el valor. */
    private val beanIrrelevante = Any()

    @BeforeEach
    fun mockearI18n() {
        i18n = Mockito.mockStatic(I18n::class.java, Mockito.withSettings().strictness(Strictness.LENIENT))
        i18n!!.`when`<String> { I18n.get(anyString()) }.thenAnswer { it.getArgument<String>(0) }
    }

    @AfterEach
    fun cerrarMockDeI18n() {
        i18n?.close()
    }

    @Test
    fun iban_checksumCorrecto_loAcepta() {
        assertNull(Iban().validate("ES9121000418450200051332", beanIrrelevante))
    }

    @Test
    fun iban_conEspacios_loAcepta() {
        assertNull(Iban().validate("ES91 2100 0418 4502 0005 1332", beanIrrelevante))
    }

    @Test
    fun iban_checksumIncorrecto_loRechaza() {
        assertRechazado(Iban().validate("ES9121000418450200051333", beanIrrelevante))
    }

    @Test
    fun iban_prefijoDistintoDeEs_loRechaza() {
        assertRechazado(Iban().validate("FR9121000418450200051332", beanIrrelevante))
    }

    @Test
    fun iban_longitudIncorrecta_loRechaza() {
        assertRechazado(Iban().validate("ES912100041845020005133", beanIrrelevante))
    }

    @Test
    fun iban_valorNuloOEnBlanco_loAcepta() {
        val regla = Iban()

        assertNull(regla.validate(null, beanIrrelevante))
        assertNull(regla.validate("   ", beanIrrelevante))
    }

    private fun assertRechazado(mensajes: BusinessMessages?) {
        assertEquals(1, mensajes?.size, "Se esperaba exactamente un mensaje de error")
    }
}
