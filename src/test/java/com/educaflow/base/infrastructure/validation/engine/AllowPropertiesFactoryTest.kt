package com.educaflow.base.infrastructure.validation.engine

import com.axelor.db.modelservice.BusinessMessages
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import kotlin.reflect.KFunction

/**
 * Tests de caracterización de `getAllowProperties` y, a través de ella, de la función privada `joinAllowProperties`,
 * que se usa cuando dos [FieldValidationRules] de la lista se refieren al mismo campo.
 */
class AllowPropertiesFactoryTest {

    /** Bean de prueba cuyos getters solo aportan el nombre del campo: `getAllowProperties` nunca los invoca. */
    @Suppress("unused")
    class BeanDePrueba {
        fun getA(): Any? = null
        fun getB(): Any? = null
        fun getC(): Any? = null
        fun getX(): Any? = null
        fun getY(): Any? = null
    }

    private val reglaNoDeCampo = object : ValidationRule {
        override fun validate(value: Any?, bean: Any): BusinessMessages? = null
    }

    private fun campo(getter: KFunction<*>, vararg reglas: ValidationRule): FieldValidationRules =
        FieldValidationRules(getter, reglas.toList())

    @Test
    fun listaVaciaDevuelveNull() {
        assertNull(getAllowProperties(emptyList()))
    }

    @Test
    fun reglasQueNoSonDeCampoSeIgnoran() {
        assertNull(getAllowProperties(listOf(reglaNoDeCampo, null)))
    }

    @Test
    fun campoSinSubcamposQuedaConValorNull() {
        val resultado = getAllowProperties(listOf(campo(BeanDePrueba::getA, reglaNoDeCampo)))

        assertEquals(mapOf("a" to null), resultado)
    }

    @Test
    fun campoRepetidoSinSubcamposEnNingunoQuedaConValorNull() {
        val resultado = getAllowProperties(listOf(campo(BeanDePrueba::getA), campo(BeanDePrueba::getA)))

        assertEquals(mapOf("a" to null), resultado)
    }

    @Test
    fun campoRepetidoConSubcamposDistintosLosUne() {
        val resultado = getAllowProperties(listOf(
            campo(BeanDePrueba::getA, campo(BeanDePrueba::getB)),
            campo(BeanDePrueba::getA, campo(BeanDePrueba::getC)),
        ))

        assertEquals(mapOf("a" to mapOf("b" to null, "c" to null)), resultado)
    }

    @Test
    fun campoRepetidoConElMismoSubcampoHojaLoDejaUnaVez() {
        val resultado = getAllowProperties(listOf(
            campo(BeanDePrueba::getA, campo(BeanDePrueba::getB)),
            campo(BeanDePrueba::getA, campo(BeanDePrueba::getB)),
        ))

        assertEquals(mapOf("a" to mapOf("b" to null)), resultado)
    }

    @Test
    fun subcampoHojaSeguidoDeSubcampoConHijosTomaLosHijos() {
        val resultado = getAllowProperties(listOf(
            campo(BeanDePrueba::getA, campo(BeanDePrueba::getB)),
            campo(BeanDePrueba::getA, campo(BeanDePrueba::getB, campo(BeanDePrueba::getX))),
        ))

        assertEquals(mapOf("a" to mapOf("b" to mapOf("x" to null))), resultado)
    }

    @Test
    fun subcampoConHijosEnAmbosLosUneRecursivamente() {
        val resultado = getAllowProperties(listOf(
            campo(BeanDePrueba::getA, campo(BeanDePrueba::getB, campo(BeanDePrueba::getX))),
            campo(BeanDePrueba::getA, campo(BeanDePrueba::getB, campo(BeanDePrueba::getY))),
        ))

        assertEquals(mapOf("a" to mapOf("b" to mapOf("x" to null, "y" to null))), resultado)
    }

    @Test
    fun subcampoConHijosSeguidoDeSubcampoHojaConservaLosHijos() {
        val resultado = getAllowProperties(listOf(
            campo(BeanDePrueba::getA, campo(BeanDePrueba::getB, campo(BeanDePrueba::getX))),
            campo(BeanDePrueba::getA, campo(BeanDePrueba::getB)),
        ))

        assertEquals(mapOf("a" to mapOf("b" to mapOf("x" to null))), resultado)
    }

    @Test
    fun campoConSubcamposSeguidoDeCampoSinSubcamposConservaLosSubcampos() {
        val resultado = getAllowProperties(listOf(
            campo(BeanDePrueba::getA, campo(BeanDePrueba::getB)),
            campo(BeanDePrueba::getA),
        ))

        assertEquals(mapOf("a" to mapOf("b" to null)), resultado)
    }

    @Test
    fun campoSinSubcamposSeguidoDeCampoConSubcamposTomaLosSubcampos() {
        val resultado = getAllowProperties(listOf(
            campo(BeanDePrueba::getA),
            campo(BeanDePrueba::getA, campo(BeanDePrueba::getB)),
        ))

        assertEquals(mapOf("a" to mapOf("b" to null)), resultado)
    }
}
