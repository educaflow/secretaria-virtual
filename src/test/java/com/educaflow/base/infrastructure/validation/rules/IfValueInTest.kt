package com.educaflow.base.infrastructure.validation.rules

import com.axelor.db.modelservice.BusinessMessages
import com.educaflow.base.infrastructure.validation.engine.ValidationRule
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Tests de [IfValueIn]. La regla lee otro campo del bean y, si su valor está en la lista, aplica las
 * reglas anidadas y junta sus mensajes; si no está en la lista, da el campo por válido sin evaluarlas.
 * Las reglas anidadas son dobles que devuelven un resultado fijo y apuntan con qué se las llamó.
 */
class IfValueInTest {

    /** Bean de prueba: el campo del que depende la regla se lee con la referencia a [estado]. */
    class Solicitud(private val estado: String?) {
        fun estado(): String? = estado
    }

    /** Regla anidada que devuelve siempre [resultado] y guarda los argumentos de cada llamada. */
    private class ReglaFija(private val resultado: BusinessMessages?) : ValidationRule {
        val llamadas = mutableListOf<Pair<Any?, Any>>()

        override fun validate(value: Any?, bean: Any): BusinessMessages? {
            llamadas.add(value to bean)
            return resultado
        }
    }

    private val valor = "valor del campo"

    private fun regla(dependentValues: List<Any>, vararg reglas: ValidationRule) =
        IfValueIn(Solicitud::estado, dependentValues, reglas.toList())

    @Test
    fun valorDependienteFueraDeLaLista_noEvaluaLasReglasYDevuelveNull() {
        val anidada = ReglaFija(BusinessMessages.single("error"))

        val resultado = regla(listOf("BORRADOR", "CERRADO"), anidada).validate(valor, Solicitud("ABIERTO"))

        assertNull(resultado)
        assertTrue(anidada.llamadas.isEmpty())
    }

    @Test
    fun valorDependienteNulo_noEstaEnLaLista_noEvaluaLasReglasYDevuelveNull() {
        val anidada = ReglaFija(BusinessMessages.single("error"))

        val resultado = regla(listOf("BORRADOR"), anidada).validate(valor, Solicitud(null))

        assertNull(resultado)
        assertTrue(anidada.llamadas.isEmpty())
    }

    @Test
    fun valorDependienteEnLaLista_sinReglas_devuelveMensajesVacios() {
        val resultado = regla(listOf("BORRADOR")).validate(valor, Solicitud("BORRADOR"))

        assertNotNull(resultado)
        assertTrue(resultado!!.isEmpty())
    }

    @Test
    fun reglasQueDevuelvenNullOVacio_noAportanMensajes() {
        val devuelveNull = ReglaFija(null)
        val devuelveVacio = ReglaFija(BusinessMessages())

        val resultado = regla(listOf("BORRADOR"), devuelveNull, devuelveVacio).validate(valor, Solicitud("BORRADOR"))

        assertTrue(resultado!!.isEmpty())
        assertEquals(1, devuelveNull.llamadas.size)
        assertEquals(1, devuelveVacio.llamadas.size)
    }

    @Test
    fun variasReglasConMensajes_losJuntaEnOrdenYLesPasaValorYBean() {
        val bean = Solicitud("CERRADO")
        val primera = ReglaFija(BusinessMessages.single("primero"))
        val sinMensajes = ReglaFija(null)
        val segunda = ReglaFija(BusinessMessages.single("segundo"))

        val resultado = regla(listOf("BORRADOR", "CERRADO"), primera, sinMensajes, segunda).validate(valor, bean)

        assertEquals(listOf("primero", "segundo"), resultado!!.map { it.message })
        assertEquals(valor, primera.llamadas.single().first)
        assertSame(bean, primera.llamadas.single().second)
        assertSame(bean, segunda.llamadas.single().second)
    }
}
