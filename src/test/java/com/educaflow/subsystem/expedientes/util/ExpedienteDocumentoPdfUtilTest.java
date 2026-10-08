package com.educaflow.subsystem.expedientes.util;

import com.educaflow.base.infrastructure.pdf.DocumentoPdf;
import com.educaflow.subsystem.expedientes.db.Expediente;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El documento de un expediente se genera en el idioma del expediente, no en el del usuario
 * autenticado (que aquí ni siquiera existe: no hay sesión).
 */
class ExpedienteDocumentoPdfUtilTest {

    /** Un tipo de expediente de prueba: el recurso se busca junto a la clase de la entidad. */
    static class ExpedienteDePrueba extends Expediente {
    }

    @Test
    void elDocumentoSaleEnElIdiomaDelExpedienteAunqueNoHayaUsuario() {
        assertSoloSale("ca", "valencia.", "castellano.");
        assertSoloSale("es", "castellano.", "valencia.");
    }

    @Test
    void sinIdiomaEnElExpedienteSaleEnCastellano() {
        assertSoloSale(null, "castellano.", "valencia.");
    }

    private static void assertSoloSale(String idioma, String esperado, String noEsperado) {
        Expediente expediente = new ExpedienteDePrueba();
        expediente.setIdioma(idioma);

        String texto = textoDe(ExpedienteDocumentoPdfUtil.getDocumentoPdf(expediente, "documento_idioma.xml"));

        assertTrue(texto.contains(esperado), "Con idioma " + idioma + " falta «" + esperado + "»: " + texto);
        assertFalse(texto.contains(noEsperado), "Con idioma " + idioma + " sobra «" + noEsperado + "»: " + texto);
    }

    private static String textoDe(DocumentoPdf documentoPdf) {
        try (PdfDocument pdf = new PdfDocument(new PdfReader(new ByteArrayInputStream(documentoPdf.getDatos())))) {
            return PdfTextExtractor.getTextFromPage(pdf.getFirstPage());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
