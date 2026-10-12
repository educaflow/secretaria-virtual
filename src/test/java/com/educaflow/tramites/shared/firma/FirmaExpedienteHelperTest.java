package com.educaflow.tramites.shared.firma;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.axelor.db.modelservice.ModelServiceFactory;
import com.axelor.meta.db.MetaFile;
import com.educaflow.base.infrastructure.metafile.MetaFileHelper;
import com.educaflow.base.infrastructure.pdf.CampoFirma;
import com.educaflow.base.infrastructure.pdf.DocumentoPdf;
import com.educaflow.base.infrastructure.pdf.ResultadoFirma;
import com.educaflow.subsystem.criptografia.db.CertificadoDigital;
import com.educaflow.subsystem.criptografia.service.CertificadoDigitalService;
import com.educaflow.subsystem.criptografia.service.FirmaEnServidorService;
import com.educaflow.subsystem.criptografia.service.SituacionFirma;
import com.educaflow.subsystem.expedientes.db.ExpedienteResoluble;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FirmaExpedienteHelperTest {

    private static final String DNI = "93882914L";
    private static final String CLAVE = "clave";
    private static final String CAMPO_FIRMA = "firmaSolicitante";

    @Mock
    private FirmaEnServidorService firmaEnServidorService;

    @Mock
    private ModelServiceFactory modelServiceFactory;

    @Mock
    private CertificadoDigitalService certificadoDigitalService;

    @InjectMocks
    private FirmaExpedienteHelper firmaExpedienteHelper;

    private MockedStatic<MetaFileHelper> metaFileHelperMock;

    private final ExpedienteResoluble expediente = new ExpedienteResoluble();
    private final MetaFile pdfOriginalMetaFile = new MetaFile();
    private final MetaFile pdfFirmadoMetaFile = new MetaFile();
    private final DocumentoPdf pdfOriginal = mock(DocumentoPdf.class);
    private final DocumentoPdf pdfFirmado = mock(DocumentoPdf.class);

    @BeforeEach
    void mockearEstaticos() {
        metaFileHelperMock = Mockito.mockStatic(MetaFileHelper.class);
    }

    @AfterEach
    void cerrarEstaticos() {
        metaFileHelperMock.close();
    }

    private void stubSituacionFirma(SituacionFirma situacionFirma) {
        when(modelServiceFactory.resolve(CertificadoDigital.class)).thenReturn(certificadoDigitalService);
        when(certificadoDigitalService.getSituacionFirmaByDni(DNI)).thenReturn(situacionFirma);
    }

    private void stubFirmaEnServidor() {
        stubSituacionFirma(SituacionFirma.FICHERO_SIN_CLAVE);
        metaFileHelperMock.when(() -> MetaFileHelper.getDocumentoPdf(pdfOriginalMetaFile)).thenReturn(pdfOriginal);
        metaFileHelperMock.when(() -> MetaFileHelper.createMetaFile(pdfFirmado)).thenReturn(pdfFirmadoMetaFile);
        when(firmaEnServidorService.firmar(eq(DNI), eq(CLAVE), eq(pdfOriginal), any(CampoFirma.class))).thenReturn(pdfFirmado);
    }

    @Test
    void firmarSolicitudEnServidor_firmaEnServidor_dejaLaSolicitudFirmadaEnSuCampoSinTocarLaOriginal() {
        expediente.setPdfSolicitud(pdfOriginalMetaFile);
        expediente.setClaveCertificado(CLAVE);
        stubFirmaEnServidor();

        firmaExpedienteHelper.firmarSolicitudEnServidor(DNI, expediente, CAMPO_FIRMA);

        assertAll(
                () -> assertSame(pdfFirmadoMetaFile, expediente.getPdfSolicitudFirmada()),
                () -> assertSame(pdfOriginalMetaFile, expediente.getPdfSolicitud()));
        verify(firmaEnServidorService).firmar(eq(DNI), eq(CLAVE), eq(pdfOriginal), Mockito.argThat(campoFirma -> CAMPO_FIRMA.equals(campoFirma.getNombreCampo())));
    }

    @Test
    void firmarSolicitudEnServidor_firmaConAutoFirma_noTocaLaSolicitudQueYaLlegoFirmada() {
        MetaFile firmadaEnElEquipoDelUsuario = new MetaFile();
        expediente.setPdfSolicitud(pdfOriginalMetaFile);
        expediente.setPdfSolicitudFirmada(firmadaEnElEquipoDelUsuario);
        stubSituacionFirma(SituacionFirma.SIN_CERTIFICADO);

        firmaExpedienteHelper.firmarSolicitudEnServidor(DNI, expediente, CAMPO_FIRMA);

        assertAll(
                () -> assertSame(firmadaEnElEquipoDelUsuario, expediente.getPdfSolicitudFirmada()),
                () -> assertSame(pdfOriginalMetaFile, expediente.getPdfSolicitud()));
        verifyNoInteractions(firmaEnServidorService);
    }

    @Test
    void firmarResolucionEnServidor_firmaEnServidor_dejaLaResolucionFirmadaEnSuCampoSinTocarLaOriginal() {
        expediente.setPdfResolucion(pdfOriginalMetaFile);
        expediente.setClaveCertificado(CLAVE);
        stubFirmaEnServidor();

        firmaExpedienteHelper.firmarResolucionEnServidor(DNI, expediente, CAMPO_FIRMA);

        assertAll(
                () -> assertSame(pdfFirmadoMetaFile, expediente.getPdfResolucionFirmada()),
                () -> assertSame(pdfOriginalMetaFile, expediente.getPdfResolucion()));
    }

    @Test
    void firmarResolucionEnServidor_firmaConAutoFirma_noTocaNada() {
        expediente.setPdfResolucion(pdfOriginalMetaFile);
        stubSituacionFirma(SituacionFirma.SIN_CERTIFICADO);

        firmaExpedienteHelper.firmarResolucionEnServidor(DNI, expediente, CAMPO_FIRMA);

        assertAll(
                () -> assertNull(expediente.getPdfResolucionFirmada()),
                () -> assertSame(pdfOriginalMetaFile, expediente.getPdfResolucion()));
        verifyNoInteractions(firmaEnServidorService);
    }

    @Test
    void actualizarFechaHoraPdfSolicitudFirmada_ponelaFechaDeLaUltimaFirmaDelPdf() {
        LocalDateTime fechaPrimeraFirma = LocalDateTime.of(2026, 9, 1, 10, 0);
        LocalDateTime fechaUltimaFirma = LocalDateTime.of(2026, 9, 2, 12, 30);
        List<ResultadoFirma> firmas = List.of(firmaConFecha(fechaUltimaFirma), firmaConFecha(fechaPrimeraFirma));
        expediente.setPdfSolicitudFirmada(pdfFirmadoMetaFile);
        metaFileHelperMock.when(() -> MetaFileHelper.getDocumentoPdf(pdfFirmadoMetaFile)).thenReturn(pdfFirmado);
        when(pdfFirmado.getFirmasPdf()).thenReturn(firmas);

        firmaExpedienteHelper.actualizarFechaHoraPdfSolicitudFirmada(expediente);

        assertEquals(fechaUltimaFirma, expediente.getFechaHoraPdfSolicitudFirmada());
    }

    @Test
    void actualizarFechaHoraPdfResolucionFirmada_ponelaFechaDeLaUltimaFirmaDelPdf() {
        LocalDateTime fechaSecretario = LocalDateTime.of(2026, 9, 1, 10, 0);
        LocalDateTime fechaDirector = LocalDateTime.of(2026, 9, 2, 12, 30);
        List<ResultadoFirma> firmas = List.of(firmaConFecha(fechaSecretario), firmaConFecha(fechaDirector));
        expediente.setPdfResolucionFirmada(pdfFirmadoMetaFile);
        metaFileHelperMock.when(() -> MetaFileHelper.getDocumentoPdf(pdfFirmadoMetaFile)).thenReturn(pdfFirmado);
        when(pdfFirmado.getFirmasPdf()).thenReturn(firmas);

        firmaExpedienteHelper.actualizarFechaHoraPdfResolucionFirmada(expediente);

        assertEquals(fechaDirector, expediente.getFechaHoraPdfResolucionFirmada());
    }

    @Test
    void actualizarFechaHoraPdfSolicitudFirmada_pdfSinFirmas_lanzaIllegalState() {
        expediente.setPdfSolicitudFirmada(pdfFirmadoMetaFile);
        metaFileHelperMock.when(() -> MetaFileHelper.getDocumentoPdf(pdfFirmadoMetaFile)).thenReturn(pdfFirmado);
        when(pdfFirmado.getFirmasPdf()).thenReturn(List.of());

        assertThrows(IllegalStateException.class, () -> firmaExpedienteHelper.actualizarFechaHoraPdfSolicitudFirmada(expediente));
    }

    private static ResultadoFirma firmaConFecha(LocalDateTime fechaFirma) {
        ResultadoFirma resultadoFirma = mock(ResultadoFirma.class);
        when(resultadoFirma.getFechaFirma()).thenReturn(fechaFirma);
        return resultadoFirma;
    }
}
