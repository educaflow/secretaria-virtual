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
 * Tests de [PostalCode]. Se comprueba qué valores acepta y cuáles rechaza, nunca el texto concreto del
 * mensaje: ese literal puede cambiar sin que cambie la regla. `I18n` se mockea porque sin contexto de
 * Axelor arrancado la traducción no está disponible.
 */
class AddressRulesTest {

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
    fun postalCode_cincoDigitos_loAcepta() {
        assertNull(PostalCode().validate("28001", beanIrrelevante))
    }

    @Test
    fun postalCode_provinciaLimiteCeutaYMelilla_loAcepta() {
        val regla = PostalCode()

        assertNull(regla.validate("01001", beanIrrelevante)) // Álava, límite inferior
        assertNull(regla.validate("51001", beanIrrelevante)) // Ceuta
        assertNull(regla.validate("52001", beanIrrelevante)) // Melilla, límite superior
    }

    @Test
    fun postalCode_codigoDeProvinciaFueraDeRango_loRechaza() {
        val regla = PostalCode()

        assertRechazado(regla.validate("00001", beanIrrelevante))
        assertRechazado(regla.validate("53001", beanIrrelevante))
        assertRechazado(regla.validate("99001", beanIrrelevante))
    }

    @Test
    fun postalCode_longitudIncorrecta_loRechaza() {
        assertRechazado(PostalCode().validate("2800", beanIrrelevante))
    }

    @Test
    fun postalCode_noNumerico_loRechaza() {
        assertRechazado(PostalCode().validate("2800A", beanIrrelevante))
    }

    @Test
    fun postalCode_valorNuloOEnBlanco_loAcepta() {
        val regla = PostalCode()

        assertNull(regla.validate(null, beanIrrelevante))
        assertNull(regla.validate("   ", beanIrrelevante))
    }

    private fun assertRechazado(mensajes: BusinessMessages?) {
        assertEquals(1, mensajes?.size, "Se esperaba exactamente un mensaje de error")
    }
}
