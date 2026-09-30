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
 * Tests de [IfLambda]. La regla evalúa la condición sobre el bean entero y, si es `true`, aplica las
 * reglas anidadas y junta sus mensajes; si es `false`, da el campo por válido sin evaluarlas.
 * Las reglas anidadas son dobles que devuelven un resultado fijo y apuntan con qué se las llamó.
 */
class IfLambdaTest {

    /** Bean de prueba sobre el que se evalúa la condición. */
    class Solicitud(val rechazada: Boolean)

    /** Regla anidada que devuelve siempre [resultado] y guarda los argumentos de cada llamada. */
    private class ReglaFija(private val resultado: BusinessMessages?) : ValidationRule {
        val llamadas = mutableListOf<Pair<Any?, Any>>()

        override fun validate(value: Any?, bean: Any): BusinessMessages? {
            llamadas.add(value to bean)
            return resultado
        }
    }

    private val valor = "valor del campo"

    private fun regla(vararg reglas: ValidationRule) =
        IfLambda<Solicitud>({ it.rechazada }, reglas.toList())

    @Test
    fun condicionFalsa_noEvaluaLasReglasYDevuelveNull() {
        val anidada = ReglaFija(BusinessMessages.single("error"))

        val resultado = regla(anidada).validate(valor, Solicitud(false))

        assertNull(resultado)
        assertTrue(anidada.llamadas.isEmpty())
    }

    @Test
    fun condicionRecibeElBeanEntero() {
        val bean = Solicitud(true)
        val recibidos = mutableListOf<Solicitud>()

        IfLambda<Solicitud>({ recibidos.add(it); false }, emptyList()).validate(valor, bean)

        assertSame(bean, recibidos.single())
    }

    @Test
    fun condicionVerdadera_sinReglas_devuelveMensajesVacios() {
        val resultado = regla().validate(valor, Solicitud(true))

        assertNotNull(resultado)
        assertTrue(resultado!!.isEmpty())
    }

    @Test
    fun reglasQueDevuelvenNullOVacio_noAportanMensajes() {
        val devuelveNull = ReglaFija(null)
        val devuelveVacio = ReglaFija(BusinessMessages())

        val resultado = regla(devuelveNull, devuelveVacio).validate(valor, Solicitud(true))

        assertTrue(resultado!!.isEmpty())
        assertEquals(1, devuelveNull.llamadas.size)
        assertEquals(1, devuelveVacio.llamadas.size)
    }

    @Test
    fun variasReglasConMensajes_losJuntaEnOrdenYLesPasaValorYBean() {
        val bean = Solicitud(true)
        val primera = ReglaFija(BusinessMessages.single("primero"))
        val sinMensajes = ReglaFija(null)
        val segunda = ReglaFija(BusinessMessages.single("segundo"))

        val resultado = regla(primera, sinMensajes, segunda).validate(valor, bean)

        assertEquals(listOf("primero", "segundo"), resultado!!.map { it.message })
        assertEquals(valor, primera.llamadas.single().first)
        assertSame(bean, primera.llamadas.single().second)
        assertSame(bean, segunda.llamadas.single().second)
    }
}
