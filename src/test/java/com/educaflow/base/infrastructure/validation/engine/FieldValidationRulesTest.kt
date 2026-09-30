package com.educaflow.base.infrastructure.validation.engine

import com.axelor.db.annotations.Widget
import com.axelor.db.modelservice.BusinessMessage
import com.axelor.db.modelservice.BusinessMessages
import com.axelor.i18n.I18n
import com.educaflow.base.util.TextUtil
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.quality.Strictness
import kotlin.reflect.KFunction

/**
 * Tests de caracterización de [FieldValidationRules]:
 * - [FieldValidationRules.getFieldName], que deduce el nombre del campo a partir del nombre del getter (`getX`/`isX`)
 *   con la convención de JavaBeans para los nombres que empiezan por dos mayúsculas.
 * - [FieldValidationRules.validate], que aplica las reglas al campo y, si una regla es a su vez un
 *   [FieldValidationRules], la aplica al objeto (o a cada elemento de la lista) anteponiendo el campo y la etiqueta del padre.
 *
 * `I18n` se mockea como identidad porque sin contexto de Axelor arrancado la traducción no está disponible.
 */
class FieldValidationRulesTest {

    private var i18n: MockedStatic<I18n>? = null

    @BeforeEach
    fun mockearI18n() {
        // lenient: los tests de getFieldName no pasan por I18n.
        i18n = Mockito.mockStatic(I18n::class.java, Mockito.withSettings().strictness(Strictness.LENIENT))
        i18n!!.`when`<String> { I18n.get(anyString()) }.thenAnswer { it.getArgument<String>(0) }
    }

    @AfterEach
    fun cerrarMockDeI18n() {
        i18n?.close()
    }

    class Telefono(@field:Widget(title = "Número") private val numero: String?) {
        fun getNumero(): String? = numero
    }

    class Direccion(@field:Widget(title = "Calle") private val calle: String?) {
        fun getCalle(): String? = calle
    }

    open class PersonaBase(private val apellidos: String?) {
        fun getApellidos(): String? = apellidos
    }

    class Persona(
        private val nombre: String?,
        @field:Widget(title = "Dirección") private val direccion: Direccion?,
        @field:Widget(title = "Teléfonos") private val telefonos: List<Telefono?>?,
        apellidos: String? = null,
    ) : PersonaBase(apellidos) {
        fun getNombre(): String? = nombre
        fun getDireccion(): Direccion? = direccion
        fun getTelefonos(): List<Telefono?>? = telefonos
        fun getInexistente(): String? = null
    }

    /** Regla que anota con qué valor y bean se la llama y devuelve siempre los mensajes fijados. */
    private class ReglaFija(private val resultado: BusinessMessages?) : ValidationRule {
        val llamadas = mutableListOf<Pair<Any?, Any>>()
        override fun validate(value: Any?, bean: Any): BusinessMessages? {
            llamadas.add(value to bean)
            return resultado
        }
    }

    private fun mensajes(vararg mensajes: BusinessMessage): BusinessMessages {
        val result = BusinessMessages()
        mensajes.forEach { result.add(it) }
        return result
    }

    private fun error(texto: String) = mensajes(BusinessMessage(null, texto, null))

    @Test
    fun validate_sinReglasDevuelveNull() {
        val persona = Persona("Ana", null, null)

        assertNull(FieldValidationRules(Persona::getNombre, emptyList()).validate("Ana", persona))
    }

    @Test
    fun validate_reglaSimpleCorrectaDevuelveNullYRecibeValorYBean() {
        val persona = Persona("Ana", null, null)
        val regla = ReglaFija(null)

        assertNull(FieldValidationRules(Persona::getNombre, listOf(regla)).validate("Ana", persona))
        assertEquals(1, regla.llamadas.size)
        assertEquals("Ana", regla.llamadas[0].first)
        assertSame(persona, regla.llamadas[0].second)
    }

    @Test
    fun validate_reglaSimpleConErrorPoneElCampoYLaEtiquetaHumanizadaSinWidget() {
        val persona = Persona(null, null, null)

        val result = FieldValidationRules(Persona::getNombre, listOf(ReglaFija(error("Obligatorio")))).validate(null, persona)

        assertEquals(mensajes(BusinessMessage("nombre", "Obligatorio", TextUtil.humanize("nombre"))), result)
    }

    @Test
    fun validate_campoHeredadoDeLaSuperclaseSeEncuentra() {
        val persona = Persona(null, null, null)

        val result = FieldValidationRules(Persona::getApellidos, listOf(ReglaFija(error("Obligatorio")))).validate(null, persona)

        assertEquals(mensajes(BusinessMessage("apellidos", "Obligatorio", TextUtil.humanize("apellidos"))), result)
    }

    @Test
    fun validate_campoInexistenteLanzaExcepcion() {
        val persona = Persona(null, null, null)
        val rules = FieldValidationRules(Persona::getInexistente, listOf(ReglaFija(null)))

        val ex = assertThrows(IllegalArgumentException::class.java) { rules.validate(null, persona) }
        assertEquals("El campo 'inexistente' no existe en la clase Persona ni en sus superclases", ex.message)
    }

    @Test
    fun validate_variasReglasAcumulanLosMensajesYUsanElTituloDelWidget() {
        val direccion = Direccion("Mayor")
        val persona = Persona(null, direccion, null)
        val rules = FieldValidationRules(
            Persona::getDireccion,
            listOf(ReglaFija(error("Uno")), ReglaFija(null), ReglaFija(error("Dos"))),
        )

        val result = rules.validate(direccion, persona)

        assertEquals(
            mensajes(
                BusinessMessage("direccion", "Uno", "Dirección"),
                BusinessMessage("direccion", "Dos", "Dirección"),
            ),
            result,
        )
    }

    @Test
    fun validate_reglaAnidadaConValorNuloSeSaltaSinLlamarla() {
        val interna = ReglaFija(error("Obligatorio"))
        val rules = FieldValidationRules(Persona::getDireccion, listOf(FieldValidationRules(Direccion::getCalle, listOf(interna))))

        assertNull(rules.validate(null, Persona(null, null, null)))
        assertEquals(0, interna.llamadas.size)
    }

    @Test
    fun validate_reglaAnidadaSobreObjetoAplicaLaReglaAlCampoInternoYAnteponeElPadre() {
        val direccion = Direccion("Mayor")
        val interna = ReglaFija(error("Muy corta"))
        val rules = FieldValidationRules(Persona::getDireccion, listOf(FieldValidationRules(Direccion::getCalle, listOf(interna))))

        val result = rules.validate(direccion, Persona(null, direccion, null))

        assertEquals(mensajes(BusinessMessage("direccion.calle", "Muy corta", "Dirección Calle")), result)
        assertEquals(1, interna.llamadas.size)
        assertEquals("Mayor", interna.llamadas[0].first)
        assertSame(direccion, interna.llamadas[0].second)
    }

    @Test
    fun validate_reglaAnidadaSobreObjetoSinErroresDevuelveNull() {
        val direccion = Direccion("Mayor")
        val rules = FieldValidationRules(Persona::getDireccion, listOf(FieldValidationRules(Direccion::getCalle, listOf(ReglaFija(null)))))

        assertNull(rules.validate(direccion, Persona(null, direccion, null)))
    }

    @Test
    fun validate_reglaAnidadaSobreListaSaltaLosNulosYPoneElIndiceDeCadaElemento() {
        val primero = Telefono("111")
        val tercero = Telefono("333")
        val telefonos = listOf(primero, null, tercero)
        val interna = ReglaFija(error("Inválido"))
        val rules = FieldValidationRules(Persona::getTelefonos, listOf(FieldValidationRules(Telefono::getNumero, listOf(interna))))

        val result = rules.validate(telefonos, Persona(null, null, telefonos))

        assertEquals(
            mensajes(
                BusinessMessage("telefonos[0].numero", "Inválido", "Teléfonos [0] Número"),
                BusinessMessage("telefonos[2].numero", "Inválido", "Teléfonos [2] Número"),
            ),
            result,
        )
        assertEquals(listOf<Pair<Any?, Any>>("111" to primero, "333" to tercero), interna.llamadas)
    }

    @Test
    fun validate_reglaAnidadaSobreListaVaciaDevuelveNull() {
        val interna = ReglaFija(error("Inválido"))
        val rules = FieldValidationRules(Persona::getTelefonos, listOf(FieldValidationRules(Telefono::getNumero, listOf(interna))))

        assertNull(rules.validate(emptyList<Telefono>(), Persona(null, null, emptyList())))
        assertEquals(0, interna.llamadas.size)
    }

    /** Bean de prueba cuyos métodos solo aportan el nombre: `getFieldName` nunca los invoca. */
    @Suppress("FunctionName", "unused")
    class BeanDePrueba {
        fun getNombre(): String = ""
        fun isActivo(): Boolean = true
        fun getURL(): String = ""
        fun getA(): String = ""
        fun getA1(): String = ""
        fun getter(): String = ""
        fun issue(): String = ""
        fun get(): String = ""
        fun `is`(): Boolean = true
        fun nombre(): String = ""
    }

    private fun fieldNameOf(method: KFunction<*>): String =
        FieldValidationRules(method, emptyList()).getFieldName()

    @Test
    fun getterGetQuitaElPrefijoYPoneLaPrimeraEnMinuscula() {
        assertEquals("nombre", fieldNameOf(BeanDePrueba::getNombre))
    }

    @Test
    fun getterIsQuitaElPrefijoYPoneLaPrimeraEnMinuscula() {
        assertEquals("activo", fieldNameOf(BeanDePrueba::isActivo))
    }

    @Test
    fun dosPrimerasMayusculasSeConservanTalCual() {
        assertEquals("URL", fieldNameOf(BeanDePrueba::getURL))
    }

    @Test
    fun nombreDeUnaSolaLetraSePoneEnMinuscula() {
        assertEquals("a", fieldNameOf(BeanDePrueba::getA))
    }

    @Test
    fun segundaLetraNoMayusculaPoneLaPrimeraEnMinuscula() {
        assertEquals("a1", fieldNameOf(BeanDePrueba::getA1))
    }

    @Test
    fun prefijoSeguidoDeMinusculaTambienSeTrataComoGetter() {
        assertEquals("ter", fieldNameOf(BeanDePrueba::getter))
        assertEquals("sue", fieldNameOf(BeanDePrueba::issue))
    }

    @Test
    fun getSinNombreLanzaExcepcion() {
        val ex = assertThrows(IllegalArgumentException::class.java) { fieldNameOf(BeanDePrueba::get) }
        assertEquals("El método get no es un getter válido", ex.message)
    }

    @Test
    fun isSinNombreLanzaExcepcion() {
        val ex = assertThrows(IllegalArgumentException::class.java) { fieldNameOf(BeanDePrueba::`is`) }
        assertEquals("El método is no es un getter válido", ex.message)
    }

    @Test
    fun metodoSinPrefijoLanzaExcepcion() {
        val ex = assertThrows(IllegalArgumentException::class.java) { fieldNameOf(BeanDePrueba::nombre) }
        assertEquals("El método nombre no es un getter válido", ex.message)
    }
}
