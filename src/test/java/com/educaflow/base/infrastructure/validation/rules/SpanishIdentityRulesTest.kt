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
 * Tests de [Dni], [Nia] y [Nuss]. Se comprueba qué valores acepta y cuáles rechaza cada regla, nunca el
 * texto concreto del mensaje: ese literal puede cambiar sin que cambie la regla. `I18n` se mockea porque
 * sin contexto de Axelor arrancado la traducción no está disponible.
 */
class SpanishIdentityRulesTest {

    private var i18n: MockedStatic<I18n>? = null

    /** Segundo parámetro de `validate`: estas reglas no lo usan, solo miran el valor. */
    private val beanIrrelevante = Any()

    @BeforeEach
    fun mockearI18n() {
        // lenient: los valores que sí cumplen la regla no pasan por I18n.
        i18n = Mockito.mockStatic(I18n::class.java, Mockito.withSettings().strictness(Strictness.LENIENT))
        i18n!!.`when`<String> { I18n.get(anyString()) }.thenAnswer { it.getArgument<String>(0) }
    }

    @AfterEach
    fun cerrarMockDeI18n() {
        i18n?.close()
    }

    @Test
    fun dni_dniValido_loAcepta() {
        assertNull(Dni().validate("93882914L", beanIrrelevante))
    }

    @Test
    fun dni_nieValido_loAcepta() {
        // Dni acepta tanto DNI como NIE: DniUtil.isValid ya resuelve ambos formatos.
        assertNull(Dni().validate("X1234567L", beanIrrelevante))
    }

    @Test
    fun dni_letraDeControlIncorrecta_loRechaza() {
        assertRechazado(Dni().validate("12345678A", beanIrrelevante))
    }

    @Test
    fun dni_valorNuloOEnBlanco_loAcepta() {
        val regla = Dni()

        assertNull(regla.validate(null, beanIrrelevante))
        assertNull(regla.validate("   ", beanIrrelevante))
    }

    @Test
    fun nia_ochoDigitos_loAcepta() {
        assertNull(Nia().validate("12345678", beanIrrelevante))
    }

    @Test
    fun nia_longitudIncorrecta_loRechaza() {
        assertRechazado(Nia().validate("1234567", beanIrrelevante))
    }

    @Test
    fun nia_noNumerico_loRechaza() {
        assertRechazado(Nia().validate("1234567A", beanIrrelevante))
    }

    @Test
    fun nia_valorNuloOEnBlanco_loAcepta() {
        val regla = Nia()

        assertNull(regla.validate(null, beanIrrelevante))
        assertNull(regla.validate("", beanIrrelevante))
    }

    @Test
    fun nuss_doceDigitos_loAcepta() {
        assertNull(Nuss().validate("281234567890", beanIrrelevante))
    }

    @Test
    fun nuss_provinciaLimiteCeutaYMelilla_loAcepta() {
        val regla = Nuss()

        assertNull(regla.validate("011234567890", beanIrrelevante)) // Álava, límite inferior
        assertNull(regla.validate("511234567890", beanIrrelevante)) // Ceuta
        assertNull(regla.validate("521234567890", beanIrrelevante)) // Melilla, límite superior
    }

    @Test
    fun nuss_codigoDeProvinciaFueraDeRango_loRechaza() {
        val regla = Nuss()

        assertRechazado(regla.validate("001234567890", beanIrrelevante))
        assertRechazado(regla.validate("531234567890", beanIrrelevante))
        assertRechazado(regla.validate("991234567890", beanIrrelevante))
    }

    @Test
    fun nuss_longitudIncorrecta_loRechaza() {
        assertRechazado(Nuss().validate("2812345678", beanIrrelevante))
    }

    @Test
    fun nuss_noNumerico_loRechaza() {
        assertRechazado(Nuss().validate("28123456789A", beanIrrelevante))
    }

    @Test
    fun nuss_valorNuloOEnBlanco_loAcepta() {
        val regla = Nuss()

        assertNull(regla.validate(null, beanIrrelevante))
        assertNull(regla.validate("   ", beanIrrelevante))
    }

    private fun assertRechazado(mensajes: BusinessMessages?) {
        assertEquals(1, mensajes?.size, "Se esperaba exactamente un mensaje de error")
    }
}
