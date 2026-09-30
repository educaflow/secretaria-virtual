package com.educaflow.tramites.util.firma

import com.axelor.auth.db.User
import com.axelor.db.modelservice.BusinessMessages
import com.axelor.i18n.I18n
import com.educaflow.base.util.SecurityUtil
import com.educaflow.subsystem.criptografia.service.SituacionFirma
import com.educaflow.subsystem.criptografia.util.CertificadoDigitalHelper
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.quality.Strictness

/**
 * Tests de caracterización de [ClaveCertificadoValida]. `SecurityUtil`, `I18n` y `CertificadoDigitalHelper` se
 * mockean: la regla solo decide qué mensaje devolver según la situación de firma y si la clave es correcta.
 */
class ClaveCertificadoValidaTest {

    private val dni = "12345678Z"

    private var securityUtil: MockedStatic<SecurityUtil>? = null
    private var i18n: MockedStatic<I18n>? = null
    private var helper: MockedStatic<CertificadoDigitalHelper>? = null

    private val regla = ClaveCertificadoValida()

    @BeforeEach
    fun mockear() {
        val lenient = Mockito.withSettings().strictness(Strictness.LENIENT)
        securityUtil = Mockito.mockStatic(SecurityUtil::class.java, lenient)
        val user = User()
        user.dni = dni
        securityUtil!!.`when`<User> { SecurityUtil.getUser() }.thenReturn(user)
        i18n = Mockito.mockStatic(I18n::class.java, lenient)
        i18n!!.`when`<String> { I18n.get(anyString()) }.thenAnswer { it.getArgument<String>(0) }
        helper = Mockito.mockStatic(CertificadoDigitalHelper::class.java, lenient)
        helper!!.`when`<String> { CertificadoDigitalHelper.motivoClaveErronea(any()) }.thenReturn("motivo")
    }

    @AfterEach
    fun cerrarMocks() {
        helper?.close()
        i18n?.close()
        securityUtil?.close()
    }

    private fun situacion(situacionFirma: SituacionFirma) {
        helper!!.`when`<SituacionFirma> { CertificadoDigitalHelper.getSituacionFirmaByDni(dni) }.thenReturn(situacionFirma)
    }

    private fun claveCorrecta(clave: String?, correcta: Boolean) {
        helper!!.`when`<Boolean> { CertificadoDigitalHelper.isClaveCertificadoCorrecta(dni, clave) }.thenReturn(correcta)
    }

    private fun assertMensajeUnico(esperado: String, resultado: BusinessMessages?) {
        assertNotNull(resultado)
        assertEquals(1, resultado!!.size)
        assertEquals(esperado, resultado[0].message)
    }

    @Test
    fun validate_situacionQueNoFirmaEnServidor_lanzaIllegalState() {
        situacion(SituacionFirma.SIN_CERTIFICADO)

        val ex = assertThrows(IllegalStateException::class.java) { regla.validate("clave", Any()) }

        assertEquals(
            "ClaveCertificadoValida solo se puede usar cuando se firma en el servidor, y la situación de firma es SIN_CERTIFICADO",
            ex.message
        )
    }

    @Test
    fun validate_sinUsuarioAutenticado_calculaLaSituacionConDniNulo() {
        securityUtil!!.`when`<User> { SecurityUtil.getUser() }.thenReturn(null)
        helper!!.`when`<SituacionFirma> { CertificadoDigitalHelper.getSituacionFirmaByDni(null) }.thenReturn(SituacionFirma.SIN_DNI)

        assertThrows(IllegalStateException::class.java) { regla.validate("clave", Any()) }
    }

    @Test
    fun validate_dispositivoSinPinYClaveVacia_pideElPin() {
        situacion(SituacionFirma.DISPOSITIVO_SIN_PIN)

        assertMensajeUnico("El PIN es obligatorio", regla.validate("  ", Any()))
    }

    @Test
    fun validate_ficheroSinClaveYClaveNula_pideLaContrasenya() {
        situacion(SituacionFirma.FICHERO_SIN_CLAVE)

        assertMensajeUnico("La contraseña es obligatoria", regla.validate(null, Any()))
    }

    @Test
    fun validate_valorQueNoEsString_seTrataComoClaveVacia() {
        situacion(SituacionFirma.FICHERO_SIN_CLAVE)

        assertMensajeUnico("La contraseña es obligatoria", regla.validate(1234, Any()))
    }

    @Test
    fun validate_necesitaClaveYEsIncorrecta_devuelveElMotivo() {
        situacion(SituacionFirma.DISPOSITIVO_SIN_PIN)
        claveCorrecta("mala", false)

        assertMensajeUnico("No es posible firmar la solicitud: motivo", regla.validate("mala", Any()))
    }

    @Test
    fun validate_necesitaClaveYEsCorrecta_esValido() {
        situacion(SituacionFirma.FICHERO_SIN_CLAVE)
        claveCorrecta("buena", true)

        assertNull(regla.validate("buena", Any()))
    }

    @Test
    fun validate_claveGuardadaYVacia_noPideClaveYCompruebaLaGuardada() {
        situacion(SituacionFirma.DISPOSITIVO_CON_PIN)
        claveCorrecta(null, true)

        assertNull(regla.validate(null, Any()))
    }

    @Test
    fun validate_claveGuardadaPeroComprobacionFalla_usaElMensajeConfigurado() {
        situacion(SituacionFirma.FICHERO_CON_CLAVE)
        claveCorrecta(null, false)

        assertMensajeUnico("Error: motivo", ClaveCertificadoValida("Error: %s").validate(null, Any()))
    }
}
