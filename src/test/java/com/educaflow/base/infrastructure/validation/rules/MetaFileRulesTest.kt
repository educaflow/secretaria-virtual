package com.educaflow.base.infrastructure.validation.rules

import com.axelor.meta.db.MetaFile
import com.educaflow.base.util.MetaFileUtil
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.MockedStatic
import org.mockito.Mockito

class MetaFileRulesTest {

    private val bytesPdf = "%PDF-1.7\n%fin".toByteArray(Charsets.US_ASCII)
    private val bytesPng = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0x0D)
    private val bytesTexto = "esto no es un pdf".toByteArray(Charsets.US_ASCII)

    private var metaFileUtil: MockedStatic<MetaFileUtil>? = null

    @BeforeEach
    fun mockear() {
        metaFileUtil = Mockito.mockStatic(MetaFileUtil::class.java)
    }

    @AfterEach
    fun cerrarMocks() {
        metaFileUtil?.close()
    }

    private fun metaFile(contentTypeDeclarado: String, contenido: ByteArray): MetaFile {
        val metaFile = MetaFile()
        metaFile.fileType = contentTypeDeclarado
        metaFileUtil!!.`when`<ByteArray> { MetaFileUtil.downloadHeader(any(), anyInt()) }
            .thenAnswer { contenido.copyOf(minOf(contenido.size, it.getArgument<Int>(1))) }
        return metaFile
    }

    @Test
    fun fileType_pdfRealDeclaradoComoPdf_esValido() {
        val resultado = FileType(listOf("application/pdf")).validate(metaFile("application/pdf", bytesPdf), Any())

        assertNull(resultado)
    }

    @Test
    fun fileType_contenidoQueNoEsPdfDeclaradoComoPdf_loRechaza() {
        val resultado = FileType(listOf("application/pdf")).validate(metaFile("application/pdf", bytesTexto), Any())

        assertNotNull(resultado)
    }

    @Test
    fun fileType_pngRealEnLaLista_esValido() {
        val resultado = FileType(listOf("image/png", "application/pdf")).validate(metaFile("image/png", bytesPng), Any())

        assertNull(resultado)
    }

    @Test
    fun fileType_pngConListaSoloPdf_loRechazaSinRepetirElTipoDeclarado() {
        val resultado = FileType(listOf("application/pdf")).validate(metaFile("application/x-inventado", bytesPng), Any())

        assertNotNull(resultado)
        assertFalse(resultado.toString().contains("application/x-inventado"))
    }

    @Test
    fun fileType_noCargaElFicheroEnteroEnMemoria() {
        FileType(listOf("image/png")).validate(metaFile("image/png", bytesPng), Any())

        metaFileUtil!!.verify({ MetaFileUtil.downloadContent(any()) }, Mockito.never())
    }

    @Test
    fun fileType_valorQueNoEsMetaFile_noValida() {
        assertNull(FileType(listOf("application/pdf")).validate("texto", Any()))
    }

    @Test
    fun fileType_tipoQueNoSabeDetectar_noSePuedeConstruir() {
        assertThrows<IllegalArgumentException> { FileType(listOf("application/pdf", "image/bmp")) }
    }
}
