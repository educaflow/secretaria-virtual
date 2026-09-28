package com.educaflow.base.infrastructure.pdfgenerator.impl;

import com.educaflow.base.infrastructure.pdfgenerator.Idioma;
import com.educaflow.base.infrastructure.pdfgenerator.PdfGenerator;
import com.educaflow.base.infrastructure.pdfgenerator.PdfGeneratorFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Escribe a disco el PDF de cada documento de ejemplo para poder rasterizarlo y compararlo a ojo
 * con el que salga tras refactorizar el generador: el golden master textual fija el texto y sus
 * coordenadas, pero no el trazo de líneas, recuadros ni rellenos.
 */
class GoldenMasterVisualTest {

    private static final Path DESTINO = Path.of("build/golden-fase0");

    private final PdfGenerator generator = PdfGeneratorFactory.getPdfGenerator();

    @ParameterizedTest(name = "{0}")
    @MethodSource("documentosQueSeGeneran")
    void elPdfQuedaEscritoYNoSaleVacio(String documento) throws IOException {
        byte[] pdf = generator.generate(DocumentosDeEjemplo.xml(documento), DocumentosDeEjemplo.contexto(documento), Idioma.CASTELLANO);

        Path fichero = Files.createDirectories(DESTINO).resolve(DocumentosDeEjemplo.sinExtension(documento) + ".pdf");
        Files.write(fichero, pdf);

        assertTrue(Files.exists(fichero), "No se ha escrito " + fichero.toAbsolutePath());
        assertTrue(Files.size(fichero) > 0, "Se ha escrito vacío " + fichero.toAbsolutePath());
    }

    private static List<String> documentosQueSeGeneran() throws IOException {
        return DocumentosDeEjemplo.nombresQueSeGeneran();
    }
}
