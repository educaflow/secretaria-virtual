package com.educaflow.base.infrastructure.pdfgenerator.impl;

import com.educaflow.base.util.Idioma;
import com.educaflow.base.infrastructure.pdfgenerator.PdfGenerator;
import com.educaflow.base.infrastructure.pdfgenerator.PdfGeneratorFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

class GoldenMasterFormularioTest {

    private static final int LINEAS_DE_CONTEXTO = 3;

    private final PdfGenerator generator = PdfGeneratorFactory.getPdfGenerator();

    @ParameterizedTest(name = "{0}")
    @MethodSource("documentosQueSeGeneran")
    void elTextoYLasCoordenadasSonLasDelGolden(String documento) throws IOException {
        byte[] pdf = generator.generate(DocumentosDeEjemplo.xml(documento), DocumentosDeEjemplo.contexto(documento), Idioma.CASTELLANO);
        List<String> obtenido = TextosDelPdf.de(pdf).volcado();

        Path golden = DocumentosDeEjemplo.CARPETA.resolve(documento + ".golden");
        if (!Files.exists(golden)) {
            throw new AssertionError("Falta el golden de " + documento + "."
                    + " Créalo en " + golden.toAbsolutePath() + " con este contenido:\n\n"
                    + String.join("\n", obtenido) + "\n");
        }

        comparar(documento, Files.readAllLines(golden, StandardCharsets.UTF_8), obtenido, golden);
    }

    private static List<String> documentosQueSeGeneran() throws IOException {
        return DocumentosDeEjemplo.nombresQueSeGeneran();
    }

    // ------------------------------------------------------------------ diff

    private static void comparar(String documento, List<String> esperado, List<String> obtenido, Path golden) {
        int primera = primeraDiferencia(esperado, obtenido);
        if (primera < 0) {
            return;
        }
        throw new AssertionError("El texto de " + documento + " ya no es el de su golden " + golden + "."
                + " Primera diferencia en la línea " + (primera + 1)
                + " (golden: " + esperado.size() + " líneas; generado: " + obtenido.size() + ")."
                + " Formato de cada línea: pagina|x|y|texto.\n"
                + "  «-» es el golden, «+» es lo que se ha generado ahora:\n"
                + diff(esperado, obtenido, primera));
    }

    private static int primeraDiferencia(List<String> esperado, List<String> obtenido) {
        for (int i = 0; i < Math.max(esperado.size(), obtenido.size()); i++) {
            if (!linea(esperado, i).equals(linea(obtenido, i))) {
                return i;
            }
        }
        return -1;
    }

    private static String diff(List<String> esperado, List<String> obtenido, int primera) {
        StringBuilder diff = new StringBuilder();
        int desde = Math.max(0, primera - LINEAS_DE_CONTEXTO);
        int hasta = Math.min(Math.max(esperado.size(), obtenido.size()), primera + LINEAS_DE_CONTEXTO + 1);
        for (int i = desde; i < hasta; i++) {
            String delGolden = linea(esperado, i);
            String generado = linea(obtenido, i);
            if (delGolden.equals(generado)) {
                diff.append(String.format("  %4d   %s%n", i + 1, delGolden));
            } else {
                diff.append(String.format("  %4d - %s%n", i + 1, delGolden));
                diff.append(String.format("  %4d + %s%n", i + 1, generado));
            }
        }
        return diff.toString();
    }

    private static String linea(List<String> lineas, int indice) {
        return indice < lineas.size() ? lineas.get(indice) : "«no hay más líneas»";
    }
}
