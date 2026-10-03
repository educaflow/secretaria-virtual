package com.educaflow.subsystem.pdfutilities.service.impl;

import com.axelor.db.Repository;
import com.axelor.db.modelservice.AllowProperties;
import com.educaflow.subsystem.pdfutilities.db.PdfUtilities;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfUtilitiesServiceImplTest {

    private PdfUtilitiesServiceImpl service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        service = new PdfUtilitiesServiceImpl(PdfUtilities.class, Mockito.mock(Repository.class));
    }

    @Test
    void getInfo_sinPdf_devuelveUnTextoSinLeerNingunFichero() {
        assertNotNull(service.getInfo(null));
    }

    @Test
    void getPdfTodasPosicionesFirma_sinPdf_noGeneraNingunFichero() {
        assertNull(service.getPdfTodasPosicionesFirma(new PdfUtilities()));
    }

    @Test
    void validaciones_noTienenPrecondiciones() {
        assertTrue(service.validateGetInfo(null).isEmpty());
        assertTrue(service.validateGetPdfTodasPosicionesFirma(new PdfUtilities()).isEmpty());
    }

    @Test
    void allowPropertiesGetPdfTodasPosicionesFirma_soloAdmiteElPdfYElNumeroDePagina() {
        AllowProperties allowProperties = service.allowPropertiesGetPdfTodasPosicionesFirma();

        assertTrue(allowProperties.allowProperty("pdf"));
        assertTrue(allowProperties.allowProperty("numeroPagina"));
        assertFalse(allowProperties.allowProperty("pdfFirmado"));
        assertFalse(allowProperties.allowProperty("info"));
    }
}
