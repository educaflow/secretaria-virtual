package com.educaflow.base.infrastructure.validation.rules

import com.axelor.auth.db.User
import com.axelor.i18n.I18n
import com.axelor.meta.db.MetaFile
import com.educaflow.base.infrastructure.metafile.MetaFileHelper
import com.educaflow.base.infrastructure.pdf.DocumentoPdf
import com.educaflow.base.infrastructure.pdf.DocumentoPdfUtil
import com.educaflow.base.util.SecurityUtil
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.quality.Strictness
import java.util.Optional

/**
 * Tests de caracterización de [FirmaPdf]. `SecurityUtil`, `I18n`, `MetaFileHelper` y `DocumentoPdfUtil` se
 * mockean: la regla solo decide qué comprobar y con qué DNI, la validación criptográfica es de `DocumentoPdfUtil`.
 */
class PdfRulesTest {

    /** Bean de prueba cuyo campo `original` hace de documento original que se pasa a la regla. */
    class BeanDePrueba(val original: Any?)

    private val dniValido = "12345678Z"

    private var securityUtil: MockedStatic<SecurityUtil>? = null
    private var i18n: MockedStatic<I18n>? = null
    private var metaFileHelper: MockedStatic<MetaFileHelper>? = null
    private var documentoPdfUtil: MockedStatic<DocumentoPdfUtil>? = null

    private val regla = FirmaPdf(BeanDePrueba::original)

    @BeforeEach
    fun mockear() {
        val lenient = Mockito.withSettings().strictness(Strictness.LENIENT)
        securityUtil = Mockito.mockStatic(SecurityUtil::class.java, lenient)
        i18n = Mockito.mockStatic(I18n::class.java, lenient)
        i18n!!.`when`<String> { I18n.get(anyString()) }.thenAnswer { it.getArgument<String>(0) }
        metaFileHelper = Mockito.mockStatic(MetaFileHelper::class.java, lenient)
        documentoPdfUtil = Mockito.mockStatic(DocumentoPdfUtil::class.java, lenient)
    }

    @AfterEach
    fun cerrarMocks() {
        documentoPdfUtil?.close()
        metaFileHelper?.close()
        i18n?.close()
        securityUtil?.close()
    }

    private fun usuarioConDni(dni: String?) {
        val user = User()
        user.dni = dni
        securityUtil!!.`when`<User> { SecurityUtil.getUser() }.thenReturn(user)
    }

    private fun assertMensajeDeUsuarioSinDni(resultado: com.axelor.db.modelservice.BusinessMessages?) {
        assertNotNull(resultado)
        assertEquals(1, resultado!!.size)
        assertEquals(
            "No es posible comprobar la firma porque su usuario no tiene un documento de identidad válido. Póngase en contacto con el administrador.",
            resultado[0].message
        )
    }

    @Test
    fun validate_sinUsuarioAutenticado_fallaAunqueNoHayaValor() {
        securityUtil!!.`when`<User> { SecurityUtil.getUser() }.thenReturn(null)

        assertMensajeDeUsuarioSinDni(regla.validate(null, BeanDePrueba(null)))
    }

    @Test
    fun validate_usuarioSinDni_falla() {
        usuarioConDni(null)

        assertMensajeDeUsuarioSinDni(regla.validate(MetaFile(), BeanDePrueba(MetaFile())))
    }

    @Test
    fun validate_usuarioConDniEnBlanco_falla() {
        usuarioConDni("   ")

        assertMensajeDeUsuarioSinDni(regla.validate(MetaFile(), BeanDePrueba(MetaFile())))
    }

    @Test
    fun validate_usuarioConDniInvalido_falla() {
        usuarioConDni("12345678A")

        assertMensajeDeUsuarioSinDni(regla.validate(MetaFile(), BeanDePrueba(MetaFile())))
    }

    @Test
    fun validate_valorNulo_loAcepta() {
        usuarioConDni(dniValido)

        assertNull(regla.validate(null, BeanDePrueba(MetaFile())))
    }

    @Test
    fun validate_valorQueNoEsMetaFile_loAceptaSinComprobarLaFirma() {
        usuarioConDni(dniValido)

        assertNull(regla.validate("no soy un fichero", BeanDePrueba(MetaFile())))
        documentoPdfUtil!!.verifyNoInteractions()
    }

    @Test
    fun validate_originalQueNoEsMetaFile_loAceptaSinComprobarLaFirma() {
        usuarioConDni(dniValido)

        assertNull(regla.validate(MetaFile(), BeanDePrueba(null)))
        documentoPdfUtil!!.verifyNoInteractions()
    }

    @Test
    fun validate_firmaCorrecta_loAceptaYComprobandoConElDniDelUsuario() {
        usuarioConDni(dniValido)
        val original = MetaFile()
        val firmado = MetaFile()
        val pdfOriginal = Mockito.mock(DocumentoPdf::class.java)
        val pdfFirmado = Mockito.mock(DocumentoPdf::class.java)
        metaFileHelper!!.`when`<DocumentoPdf> { MetaFileHelper.getDocumentoPdf(original) }.thenReturn(pdfOriginal)
        metaFileHelper!!.`when`<DocumentoPdf> { MetaFileHelper.getDocumentoPdf(firmado) }.thenReturn(pdfFirmado)
        documentoPdfUtil!!.`when`<Optional<String>> { DocumentoPdfUtil.validateFirmaPdf(pdfOriginal, pdfFirmado, dniValido) }
            .thenReturn(Optional.empty())

        assertNull(regla.validate(firmado, BeanDePrueba(original)))
        documentoPdfUtil!!.verify { DocumentoPdfUtil.validateFirmaPdf(pdfOriginal, pdfFirmado, dniValido) }
    }

    @Test
    fun validate_firmaIncorrecta_devuelveElMensajeDeDocumentoPdfUtil() {
        usuarioConDni(dniValido)
        val pdf = Mockito.mock(DocumentoPdf::class.java)
        metaFileHelper!!.`when`<DocumentoPdf> { MetaFileHelper.getDocumentoPdf(any()) }.thenReturn(pdf)
        documentoPdfUtil!!.`when`<Optional<String>> { DocumentoPdfUtil.validateFirmaPdf(any(), any(), anyString()) }
            .thenReturn(Optional.of("La firma no es válida"))

        val resultado = regla.validate(MetaFile(), BeanDePrueba(MetaFile()))

        assertNotNull(resultado)
        assertEquals(1, resultado!!.size)
        assertEquals("La firma no es válida", resultado[0].message)
    }
}
