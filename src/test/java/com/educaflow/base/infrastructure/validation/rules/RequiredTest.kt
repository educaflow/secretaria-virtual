package com.educaflow.base.infrastructure.validation.rules

import com.axelor.db.modelservice.BusinessMessages
import com.axelor.i18n.I18n
import com.axelor.meta.db.MetaFile
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.quality.Strictness
import java.math.BigDecimal

/**
 * Tests de [Required]. Se comprueba qué valores acepta y cuáles rechaza, nunca el texto concreto del
 * mensaje: ese literal puede cambiar sin que cambie la regla. `I18n` se mockea porque sin contexto de
 * Axelor arrancado la traducción no está disponible.
 */
class RequiredTest {

    private var i18n: MockedStatic<I18n>? = null

    /** Segundo parámetro de `validate`: esta regla no lo usa, solo mira el valor. */
    private val beanIrrelevante = Any()

    @BeforeEach
    fun mockearI18n() {
        // lenient: el valor que sí cumple la regla no pasa por I18n.
        i18n = Mockito.mockStatic(I18n::class.java, Mockito.withSettings().strictness(Strictness.LENIENT))
        i18n!!.`when`<String> { I18n.get(anyString()) }.thenAnswer { it.getArgument<String>(0) }
    }

    @AfterEach
    fun cerrarMockDeI18n() {
        i18n?.close()
    }

    @Test
    fun valorPresente_loAcepta() {
        assertNull(Required().validate("12345678", beanIrrelevante))
    }

    @Test
    fun valorNulo_loRechaza() {
        assertRechazado(Required().validate(null, beanIrrelevante))
    }

    @Test
    fun textoEnBlanco_loRechaza() {
        assertRechazado(Required().validate("   ", beanIrrelevante))
    }

    @Test
    fun ficheroSinNombre_loRechaza() {
        val ficheroSinNombre = MetaFile()
        ficheroSinNombre.fileName = ""

        assertRechazado(Required().validate(ficheroSinNombre, beanIrrelevante))
    }

    @Test
    fun ficheroDeTamanoCero_loRechaza() {
        val ficheroVacio = MetaFile()
        ficheroVacio.fileName = "solicitud.pdf"
        ficheroVacio.fileSize = 0L

        assertRechazado(Required().validate(ficheroVacio, beanIrrelevante))
    }

    @Test
    fun numeroACero_loRechaza() {
        val regla = Required()

        assertRechazado(regla.validate(0, beanIrrelevante))
        assertRechazado(regla.validate(BigDecimal.ZERO, beanIrrelevante))
    }

    private fun assertRechazado(mensajes: BusinessMessages?) {
        assertEquals(1, mensajes?.size, "Se esperaba exactamente un mensaje de error")
    }
}
