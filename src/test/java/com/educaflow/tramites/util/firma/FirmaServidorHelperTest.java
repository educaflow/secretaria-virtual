package com.educaflow.tramites.util.firma;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.i18n.I18n;
import com.axelor.meta.db.MetaFile;
import com.educaflow.base.infrastructure.metafile.MetaFileHelper;
import com.educaflow.base.infrastructure.pdf.CampoFirma;
import com.educaflow.base.infrastructure.pdf.DocumentoPdf;
import com.educaflow.base.infrastructure.pdf.Rectangulo;
import com.educaflow.base.infrastructure.validation.messages.BusinessException;
import com.educaflow.base.util.DniUtil;
import com.educaflow.subsystem.criptografia.service.CredentialsFailureException;
import com.educaflow.subsystem.criptografia.service.FirmaEnServidorService;
import com.educaflow.subsystem.criptografia.service.SituacionFirma;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
class FirmaServidorHelperTest {

    private static final String DNI = "93882914L";
    private static final String CLAVE = "clave";
    private static final Rectangulo POSICION = new Rectangulo(1f, 2f, 3f, 4f);
    private static final int PAGINA = 2;

    @Mock
    private FirmaEnServidorService firmaEnServidorService;

    @InjectMocks
    private FirmaServidorHelper firmaServidorHelper;

    private MockedStatic<MetaFileHelper> metaFileHelperMock;
    private MockedStatic<I18n> i18nMock;

    private final MetaFile documentoOriginal = new MetaFile();

    @BeforeEach
    void mockearEstaticos() {
        metaFileHelperMock = Mockito.mockStatic(MetaFileHelper.class);
        i18nMock = Mockito.mockStatic(I18n.class, Mockito.withSettings().strictness(Strictness.LENIENT));
        i18nMock.when(() -> I18n.get(anyString())).thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    @AfterEach
    void cerrarEstaticos() {
        metaFileHelperMock.close();
        i18nMock.close();
    }

    @Test
    void firmarEnServidor_dniNull_lanzaNullPointer() {
        NullPointerException ex = assertThrows(NullPointerException.class,
                () -> firmaServidorHelper.firmarEnServidor(null, SituacionFirma.FICHERO_CON_CLAVE, CLAVE, documentoOriginal, POSICION, PAGINA));

        assertEquals("No hay DNI para el firmante", ex.getMessage());
        verifyNoInteractions(firmaEnServidorService);
    }

    @Test
    void firmarEnServidor_dniEnBlanco_lanzaIllegalArgument() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> firmaServidorHelper.firmarEnServidor("   ", SituacionFirma.FICHERO_CON_CLAVE, CLAVE, documentoOriginal, POSICION, PAGINA));

        assertEquals("No hay DNI para el firmante", ex.getMessage());
        verifyNoInteractions(firmaEnServidorService);
    }

    @Test
    void firmarEnServidor_dniInvalido_lanzaIllegalArgumentConElDniEnmascarado() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> firmaServidorHelper.firmarEnServidor("12345678A", SituacionFirma.FICHERO_CON_CLAVE, CLAVE, documentoOriginal, POSICION, PAGINA));

        assertEquals("El DNI no es válido:" + DniUtil.enmascarar("12345678A"), ex.getMessage());
        verifyNoInteractions(firmaEnServidorService);
    }

    @Test
    void firmarEnServidor_sinSituacionFirma_lanzaNullPointer() {
        NullPointerException ex = assertThrows(NullPointerException.class,
                () -> firmaServidorHelper.firmarEnServidor(DNI, null, CLAVE, documentoOriginal, POSICION, PAGINA));

        assertEquals("No hay situación de firma para el firmante con DNI " + DniUtil.enmascarar(DNI), ex.getMessage());
        verifyNoInteractions(firmaEnServidorService);
    }

    @Test
    void firmarEnServidor_situacionSinFirmaEnServidor_lanzaIllegalState() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> firmaServidorHelper.firmarEnServidor(DNI, SituacionFirma.SIN_CERTIFICADO, CLAVE, documentoOriginal, POSICION, PAGINA));

        assertEquals("No corresponde firmar en el servidor con la situación de firma SIN_CERTIFICADO", ex.getMessage());
        verifyNoInteractions(firmaEnServidorService);
    }

    @Test
    void firmarEnServidor_sinDocumentoOriginal_lanzaNullPointer() {
        NullPointerException ex = assertThrows(NullPointerException.class,
                () -> firmaServidorHelper.firmarEnServidor(DNI, SituacionFirma.FICHERO_CON_CLAVE, CLAVE, null, POSICION, PAGINA));

        assertEquals("No hay documento de entrada que firmar", ex.getMessage());
        verifyNoInteractions(firmaEnServidorService);
    }

    @Test
    void firmarEnServidor_datosCorrectos_firmaYDevuelveElMetaFileDelDocumentoFirmado() throws BusinessException {
        DocumentoPdf pdfOriginal = mock(DocumentoPdf.class);
        DocumentoPdf pdfFirmado = mock(DocumentoPdf.class);
        MetaFile metaFileFirmado = new MetaFile();
        metaFileHelperMock.when(() -> MetaFileHelper.getDocumentoPdf(documentoOriginal)).thenReturn(pdfOriginal);
        metaFileHelperMock.when(() -> MetaFileHelper.createMetaFile(pdfFirmado)).thenReturn(metaFileFirmado);
        when(firmaEnServidorService.firmar(eq(DNI), eq(CLAVE), eq(pdfOriginal), any(CampoFirma.class))).thenReturn(pdfFirmado);

        MetaFile resultado = firmaServidorHelper.firmarEnServidor(DNI, SituacionFirma.FICHERO_CON_CLAVE, CLAVE, documentoOriginal, POSICION, PAGINA);

        assertSame(metaFileFirmado, resultado);
        ArgumentCaptor<CampoFirma> campoFirma = ArgumentCaptor.forClass(CampoFirma.class);
        verify(firmaEnServidorService).firmar(eq(DNI), eq(CLAVE), eq(pdfOriginal), campoFirma.capture());
        assertAll(
                () -> assertEquals(POSICION, campoFirma.getValue().getRectanguloMensaje()),
                () -> assertEquals(PAGINA, campoFirma.getValue().getNumeroPagina()));
    }

    @Test
    void firmarEnServidor_conNombreDeCampoFirma_firmaEnEseCampoYSinRectangulo() throws BusinessException {
        DocumentoPdf pdfOriginal = mock(DocumentoPdf.class);
        DocumentoPdf pdfFirmado = mock(DocumentoPdf.class);
        MetaFile metaFileFirmado = new MetaFile();
        metaFileHelperMock.when(() -> MetaFileHelper.getDocumentoPdf(documentoOriginal)).thenReturn(pdfOriginal);
        metaFileHelperMock.when(() -> MetaFileHelper.createMetaFile(pdfFirmado)).thenReturn(metaFileFirmado);
        when(firmaEnServidorService.firmar(eq(DNI), eq(CLAVE), eq(pdfOriginal), any(CampoFirma.class))).thenReturn(pdfFirmado);

        MetaFile resultado = firmaServidorHelper.firmarEnServidor(DNI, SituacionFirma.FICHERO_CON_CLAVE, CLAVE, documentoOriginal, "firmaSolicitante");

        assertSame(metaFileFirmado, resultado);
        ArgumentCaptor<CampoFirma> campoFirma = ArgumentCaptor.forClass(CampoFirma.class);
        verify(firmaEnServidorService).firmar(eq(DNI), eq(CLAVE), eq(pdfOriginal), campoFirma.capture());
        assertAll(
                () -> assertEquals("firmaSolicitante", campoFirma.getValue().getNombreCampo()),
                () -> assertEquals(null, campoFirma.getValue().getRectanguloMensaje()));
    }

    @Test
    void firmarEnServidor_claveErronea_lanzaBusinessExceptionConElMotivo() {
        DocumentoPdf pdfOriginal = mock(DocumentoPdf.class);
        metaFileHelperMock.when(() -> MetaFileHelper.getDocumentoPdf(documentoOriginal)).thenReturn(pdfOriginal);
        when(firmaEnServidorService.firmar(eq(DNI), eq(CLAVE), eq(pdfOriginal), any(CampoFirma.class)))
                .thenThrow(new CredentialsFailureException(DNI, new RuntimeException("clave mala")));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> firmaServidorHelper.firmarEnServidor(DNI, SituacionFirma.FICHERO_SIN_CLAVE, CLAVE, documentoOriginal, POSICION, PAGINA));

        BusinessMessages mensajes = ex.getBusinessMessages();
        assertEquals(1, mensajes.size());
        assertEquals("No se ha podido firmar la solicitud: la contraseña indicada no es correcta", mensajes.get(0).getMessage());
        metaFileHelperMock.verify(() -> MetaFileHelper.createMetaFile(any()), Mockito.never());
    }
}
