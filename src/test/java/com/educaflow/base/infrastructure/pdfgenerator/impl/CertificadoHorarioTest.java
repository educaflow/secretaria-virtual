package com.educaflow.base.infrastructure.pdfgenerator.impl;

import com.educaflow.base.util.Idioma;
import com.educaflow.base.infrastructure.pdfgenerator.PdfGenerator;
import com.educaflow.base.infrastructure.pdfgenerator.PdfGeneratorFactory;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El certificado de horario reproduce un documento real: el que el centro emite hoy con Word está
 * versionado como {@code GonzalezLorenzo_CertificadoHorario_Curso26-27.pdf} en la raíz del proyecto.
 * El PDF generado se deja en {@link #DESTINO} para poder rasterizarlo y compararlo con ese original.
 */
class CertificadoHorarioTest {

    private static final String XML = "/com/educaflow/base/infrastructure/pdfgenerator/documentotexto/certificado/certificado_horario.xml";
    private static final Path DESTINO = Path.of("build/certificado");

    private static final Map<String, Object> SELF = Map.of("nombre", "LORENZO GONZÁLEZ GASCÓN", "dni", "24362574P");

    private static final List<String> SOLO_CASTELLANO =
            List.of("SECRETARIO", "CENTRO", "profesor", "Lunes:", "Miércoles:", "horas", "septiembre", "Fdo:", "VºBº");
    private static final List<String> SOLO_VALENCIANO =
            List.of("SECRETARI", "CENTRE", "professor", "Dilluns:", "Dimecres:", "hores", "setembre", "Signat:", "Vistiplau");

    private final PdfGenerator generator = PdfGeneratorFactory.getPdfGenerator();

    @Test
    void elCertificadoEstampaSusBloquesEnElOrdenDelOriginal() {
        TextosDelPdf textos = TextosDelPdf.de(generar(Idioma.CASTELLANO));

        assertTrue(textos.contiene("CERTIFICA:"), "No está el CERTIFICA: " + textos.volcado());
        assertTrue(textos.yDe("JAVIER") > textos.yDe("CERTIFICA:"), "El encabezamiento no va encima del CERTIFICA:");
        assertTrue(textos.yDe("CERTIFICA:") > textos.yDe("LORENZO"), "El CERTIFICA: no va encima del párrafo del interesado");
        assertTrue(textos.yDe("LORENZO") > textos.yDe("Lunes:"), "El párrafo del interesado no va encima de la lista de días");
        assertTrue(textos.yDe("Lunes:") > textos.yDe("Viernes:"), "Los días no van en orden");
        assertTrue(textos.yDe("Viernes:") > textos.yDe("mismo,"), "La lista de días no va encima del párrafo del cargo remunerado");
        assertTrue(textos.yDe("mismo,") > textos.yDe("conste,"), "Los dos párrafos de cierre no van en orden");
        assertTrue(textos.yDe("conste,") > textos.yDe("VºBº"), "El cierre no va encima del bloque de firmas");
        assertTrue(textos.yDe("VºBº") > textos.yDe("DIRECTOR"), "El VºBº no va encima del cargo que visa");
        assertTrue(textos.yDe("DIRECTOR") > textos.yDe("Olmo"), "Los cargos no van encima de los Fdo:");

        assertEquals(textos.yDe("LORENZO"), textos.yDe("GASCÓN"),
                "El nombre del interesado no sale entero en la misma línea");
        assertEquals(textos.yDe("LORENZO"), textos.yDe("24362574P"),
                "El nombre y el DNI no salen en la misma línea");
        assertEquals(5, textos.todos().stream().filter(t -> t.texto().equals("•")).count(),
                "No hay una viñeta por día: " + textos.volcado());
        assertEquals(textos.yDe("Olmo"), textos.yDe("Soria"), "Los dos Fdo: no van a la misma altura");
        assertTrue(textos.xDe("Soria") > textos.xDe("Olmo") + 150, "El secretario no va en la segunda columna");
    }

    @Test
    void elCertificadoEnCastellanoNoDejaCaerNiUnaPalabraDelValenciano() {
        assertSoloSale(TextosDelPdf.de(generar(Idioma.CASTELLANO)), SOLO_CASTELLANO, SOLO_VALENCIANO);
    }

    @Test
    void elCertificadoEnValencianoNoDejaCaerNiUnaPalabraDelCastellano() {
        assertSoloSale(TextosDelPdf.de(generar(Idioma.VALENCIANO)), SOLO_VALENCIANO, SOLO_CASTELLANO);
    }

    @Test
    void lasVariablesDelCertificadoSonElNombreYElDniDelInteresado() {
        assertEquals(List.of("self.nombre", "self.dni"), generator.getExpresiones(xml()));
    }

    private static void assertSoloSale(TextosDelPdf textos, List<String> propias, List<String> delOtroIdioma) {
        propias.forEach(palabra -> assertTrue(textos.contiene(palabra),
                "Falta «" + palabra + "»: " + textos.volcado()));
        delOtroIdioma.forEach(palabra -> assertFalse(textos.contiene(palabra),
                "Sale «" + palabra + "», que es del otro idioma: " + textos.volcado()));
    }

    private byte[] generar(Idioma idioma) {
        byte[] pdf = generator.generate(xml(), Map.of("self", SELF), idioma);
        try {
            Files.write(Files.createDirectories(DESTINO).resolve(fichero(idioma)), pdf);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
        return pdf;
    }

    private static String fichero(Idioma idioma) {
        return switch (idioma) {
            case CASTELLANO -> "certificado.pdf";
            case VALENCIANO -> "certificado-ca.pdf";
        };
    }

    private static byte[] xml() {
        try (InputStream in = CertificadoHorarioTest.class.getResourceAsStream(XML)) {
            return in.readAllBytes();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
