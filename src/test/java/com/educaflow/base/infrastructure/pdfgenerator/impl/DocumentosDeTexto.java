package com.educaflow.base.infrastructure.pdfgenerator.impl;

import com.educaflow.base.util.Idioma;
import com.educaflow.base.infrastructure.pdfgenerator.PdfGenerator;
import com.educaflow.base.infrastructure.pdfgenerator.PdfGeneratorFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Los documentos en prosa de los tests, que viven en una subcarpeta propia: {@link DocumentosDeEjemplo}
 * lista la carpeta padre y los metería en el golden master del formulario, que es de otro tipo de
 * documento.
 */
final class DocumentosDeTexto {

    private static final String CARPETA = "/com/educaflow/base/infrastructure/pdfgenerator/documentotexto/";
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 24, 10, 30);

    private static final PdfGenerator GENERATOR = PdfGeneratorFactory.getPdfGenerator();

    private DocumentosDeTexto() {
    }

    static byte[] xml(String documento) {
        try (InputStream in = DocumentosDeTexto.class.getResourceAsStream(CARPETA + documento)) {
            if (in == null) {
                throw new AssertionError("Falta el recurso de test " + CARPETA + documento);
            }
            return in.readAllBytes();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    /** El XML de un documento como texto, para generar variantes suyas sin duplicar el recurso. */
    static String fuente(String documento) {
        return new String(xml(documento), StandardCharsets.UTF_8);
    }

    static byte[] pdf(String documento, Map<String, Object> self, Idioma idioma) {
        return pdfDe(fuente(documento), self, idioma);
    }

    static byte[] pdfDe(String xml, Map<String, Object> self, Idioma idioma) {
        return GENERATOR.generate(xml.getBytes(StandardCharsets.UTF_8), Map.of("self", self, "now", NOW), idioma);
    }

    static TextosDelPdf generar(String documento) {
        return TextosDelPdf.de(pdf(documento, Map.of(), Idioma.CASTELLANO));
    }
}
