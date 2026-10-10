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
 * Tests de [Phone]. Se comprueba qué valores acepta y cuáles rechaza (delega en `NumeroTelefono`:
 * pasan los teléfonos válidos de España, fijos o móviles), nunca el texto concreto del mensaje: ese
 * literal puede cambiar sin que cambie la regla. `I18n` se mockea porque sin contexto de Axelor
 * arrancado la traducción no está disponible.
 */
class ContactRulesTest {

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
    fun phone_movilOFijoDeEspana_loAcepta() {
        val regla = Phone()

        assertNull(regla.validate("612345678", beanIrrelevante))
        assertNull(regla.validate("963000000", beanIrrelevante))
        assertNull(regla.validate("+34612345678", beanIrrelevante))
        assertNull(regla.validate("612 34 56 78", beanIrrelevante))
    }

    @Test
    fun phone_telefonoDeOtroPais_loRechaza() {
        assertRechazado(Phone().validate("+33612345678", beanIrrelevante))
    }

    @Test
    fun phone_longitudIncorrecta_loRechaza() {
        assertRechazado(Phone().validate("61234567", beanIrrelevante))
    }

    @Test
    fun phone_textoNoParseable_loRechaza() {
        assertRechazado(Phone().validate("no-es-un-telefono", beanIrrelevante))
    }

    @Test
    fun phone_valorNuloOEnBlanco_loAcepta() {
        val regla = Phone()

        assertNull(regla.validate(null, beanIrrelevante))
        assertNull(regla.validate("   ", beanIrrelevante))
    }

    private fun assertRechazado(mensajes: BusinessMessages?) {
        assertEquals(1, mensajes?.size, "Se esperaba exactamente un mensaje de error")
    }
}
