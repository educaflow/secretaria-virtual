package com.educaflow.tramites.util.firma

import com.axelor.auth.db.User
import com.axelor.db.modelservice.BusinessMessages
import com.educaflow.base.infrastructure.validation.engine.ValidationRule
import com.axelor.db.modelservice.ModelServiceFactory
import com.axelor.inject.Beans
import com.educaflow.base.util.SecurityUtil
import com.educaflow.subsystem.criptografia.db.CertificadoDigital
import com.educaflow.subsystem.criptografia.service.CertificadoDigitalService
import com.educaflow.subsystem.criptografia.service.SituacionFirma
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.quality.Strictness

/**
 * Tests de caracterización de [IfSituacionFirma]. `SecurityUtil` y `CertificadoDigitalService` se mockean para fijar
 * la situación de firma del usuario autenticado; las reglas internas son reglas de prueba que registran sus llamadas.
 */
class IfSituacionFirmaTest {

    private val dni = "93882914L"

    private var securityUtil: MockedStatic<SecurityUtil>? = null
    private var beans: MockedStatic<Beans>? = null
    private var certificadoDigitalService: CertificadoDigitalService? = null

    /** Regla de prueba que devuelve siempre el mismo resultado y guarda los argumentos con los que se la llama. */
    private class ReglaFija(private val resultado: BusinessMessages?) : ValidationRule {
        val llamadas = mutableListOf<Pair<Any?, Any>>()

        override fun validate(value: Any?, bean: Any): BusinessMessages? {
            llamadas += Pair(value, bean)
            return resultado
        }
    }

    @BeforeEach
    fun mockear() {
        val lenient = Mockito.withSettings().strictness(Strictness.LENIENT)
        securityUtil = Mockito.mockStatic(SecurityUtil::class.java, lenient)
        val user = User()
        user.dni = dni
        securityUtil!!.`when`<User> { SecurityUtil.getUser() }.thenReturn(user)
        certificadoDigitalService = Mockito.mock(CertificadoDigitalService::class.java)
        val modelServiceFactory = Mockito.mock(ModelServiceFactory::class.java)
        Mockito.`when`(modelServiceFactory.resolve(CertificadoDigital::class.java)).thenReturn(certificadoDigitalService)
        beans = Mockito.mockStatic(Beans::class.java, lenient)
        beans!!.`when`<ModelServiceFactory> { Beans.get(ModelServiceFactory::class.java) }.thenReturn(modelServiceFactory)
        Mockito.`when`(certificadoDigitalService!!.getSituacionFirmaByDni(dni)).thenReturn(SituacionFirma.FICHERO_SIN_CLAVE)
    }

    @AfterEach
    fun cerrarMocks() {
        beans?.close()
        securityUtil?.close()
    }

    @Test
    fun validate_condicionFalsa_devuelveNullSinLlamarALasReglas() {
        val regla = ReglaFija(BusinessMessages.single("error"))
        val recibidas = mutableListOf<SituacionFirma>()

        val resultado = IfSituacionFirma({ recibidas += it; false }, listOf(regla)).validate("valor", Any())

        assertNull(resultado)
        assertEquals(listOf(SituacionFirma.FICHERO_SIN_CLAVE), recibidas)
        assertTrue(regla.llamadas.isEmpty())
    }

    @Test
    fun validate_condicionVerdaderaSinReglas_devuelveMensajesVacios() {
        val resultado = IfSituacionFirma({ true }, emptyList()).validate("valor", Any())

        assertNotNull(resultado)
        assertTrue(resultado!!.isEmpty())
    }

    @Test
    fun validate_condicionVerdadera_acumulaLosMensajesDeTodasLasReglasEnOrden() {
        val bean = Any()
        val primera = ReglaFija(BusinessMessages.single("uno"))
        val segunda = ReglaFija(BusinessMessages.single("dos"))

        val resultado = IfSituacionFirma({ it.isFirmaEnServidor }, listOf(primera, segunda)).validate("valor", bean)

        assertEquals(listOf("uno", "dos"), resultado!!.map { it.message })
        assertEquals(1, primera.llamadas.size)
        assertEquals("valor", primera.llamadas[0].first)
        assertSame(bean, primera.llamadas[0].second)
        assertEquals(1, segunda.llamadas.size)
    }

    @Test
    fun validate_reglasQueDevuelvenNullOVacio_seIgnoranYDevuelveMensajesVacios() {
        val nula = ReglaFija(null)
        val vacia = ReglaFija(BusinessMessages())
        val conError = ReglaFija(BusinessMessages.single("error"))

        val resultado = IfSituacionFirma({ true }, listOf(nula, vacia, conError)).validate(null, Any())

        assertEquals(listOf("error"), resultado!!.map { it.message })
        assertEquals(1, nula.llamadas.size)
        assertEquals(1, vacia.llamadas.size)
    }

    @Test
    fun validate_sinUsuarioAutenticado_calculaLaSituacionConDniNulo() {
        securityUtil!!.`when`<User> { SecurityUtil.getUser() }.thenReturn(null)
        Mockito.`when`(certificadoDigitalService!!.getSituacionFirmaByDni(null)).thenReturn(SituacionFirma.SIN_DNI)
        val recibidas = mutableListOf<SituacionFirma>()

        val resultado = IfSituacionFirma({ recibidas += it; false }, emptyList()).validate("valor", Any())

        assertNull(resultado)
        assertEquals(listOf(SituacionFirma.SIN_DNI), recibidas)
    }
}
