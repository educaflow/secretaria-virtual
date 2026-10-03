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
 * Tests de [MinLength], [MaxLength], [NoAllUpperCase] y [Pattern]. Se comprueba qué valores acepta y cuáles rechaza cada
 * regla, nunca el texto concreto del mensaje: ese literal puede cambiar sin que cambie la regla. `I18n`
 * se mockea porque sin contexto de Axelor arrancado la traducción no está disponible.
 */
class StringRulesTest {

    private var i18n: MockedStatic<I18n>? = null

    /** Segundo parámetro de `validate`: estas reglas no lo usan, solo miran el valor. */
    private val beanIrrelevante = Any()

    @BeforeEach
    fun mockearI18n() {
        // lenient: los mensajes calculados y los valores que sí cumplen la regla no pasan por I18n.
        i18n = Mockito.mockStatic(I18n::class.java, Mockito.withSettings().strictness(Strictness.LENIENT))
        i18n!!.`when`<String> { I18n.get(anyString()) }.thenAnswer { it.getArgument<String>(0) }
    }

    @AfterEach
    fun cerrarMockDeI18n() {
        i18n?.close()
    }

    @Test
    fun minLength_longitudSuficiente_loAcepta() {
        assertNull(MinLength(5).validate("123456", beanIrrelevante))
    }

    @Test
    fun minLength_textoCorto_loRechaza() {
        assertRechazado(MinLength(5).validate("123", beanIrrelevante))
    }

    @Test
    fun minLength_valorQueNoEsTexto_loAcepta() {
        // La regla solo opina sobre cadenas; la obligatoriedad la declara `Required` aparte.
        val regla = MinLength(5)

        assertNull(regla.validate(null, beanIrrelevante))
        assertNull(regla.validate(123, beanIrrelevante))
    }

    @Test
    fun maxLength_longitudDentroDelLimite_loAcepta() {
        assertNull(MaxLength(10).validate("123", beanIrrelevante))
    }

    @Test
    fun maxLength_textoLargo_loRechaza() {
        val textoDe12Caracteres = "a".repeat(12)

        assertRechazado(MaxLength(10).validate(textoDe12Caracteres, beanIrrelevante))
    }

    @Test
    fun maxLength_valorQueNoEsTexto_loAcepta() {
        val regla = MaxLength(10)

        assertNull(regla.validate(null, beanIrrelevante))
        assertNull(regla.validate(123, beanIrrelevante))
    }

    @Test
    fun pattern_valorQueCasa_loAcepta() {
        assertNull(Pattern("^\\d{8}\$").validate("12345678", beanIrrelevante))
    }

    @Test
    fun pattern_valorQueNoCasa_loRechaza() {
        assertRechazado(Pattern("^\\d{8}\$").validate("12A45678", beanIrrelevante))
    }

    @Test
    fun pattern_valorConEspaciosAlrededor_seComparaSobreElTextoRecortado() {
        assertNull(Pattern("^\\d{8}\$").validate(" 12345678 ", beanIrrelevante))
    }

    @Test
    fun noAllUpperCase_todoEnMayusculas_loRechaza() {
        assertRechazado(NoAllUpperCase().validate("HOLA", beanIrrelevante))
    }

    @Test
    fun noAllUpperCase_conMinusculas_loAcepta() {
        assertNull(NoAllUpperCase().validate("Hola", beanIrrelevante))
    }

    @Test
    fun noAllUpperCase_sinLetras_loAcepta() {
        // Sin letras no hay mayúsculas que rechazar.
        assertNull(NoAllUpperCase().validate("2024-25", beanIrrelevante))
    }

    private fun assertRechazado(mensajes: BusinessMessages?) {
        assertEquals(1, mensajes?.size, "Se esperaba exactamente un mensaje de error")
    }
}
