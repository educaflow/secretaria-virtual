package com.educaflow.base.infrastructure.pdfgenerator.impl;

import com.educaflow.base.infrastructure.pdf.DocumentoPdf;
import com.educaflow.base.infrastructure.pdf.DocumentoPdfFactory;
import com.educaflow.base.infrastructure.pdfgenerator.PdfGenerator;
import com.educaflow.base.infrastructure.pdfgenerator.PdfGeneratorFactory;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfGeneratorImplITextTest {

    private final PdfGenerator generator = PdfGeneratorFactory.getPdfGenerator();

    @Test
    void getExpresionesIncluyeNombreCampoInlineYVisibleSinDuplicados() {
        List<String> expresiones = generator.getExpresiones(xml("documento_basico.xml"));
        assertEquals(List.of("self.nombre", "self.acepta", "self.ciudad"), expresiones);

        List<String> conVisible = generator.getExpresiones(xml("documento_colapsar.xml"));
        assertEquals(List.of("self.mostrar", "true", "self.nombre"), conVisible);
    }

    @Test
    void estampaLosValoresYNoLasExpresiones() {
        byte[] pdf = generator.generate(xml("documento_basico.xml"), contexto("nombre", "Núria Peñalver l·lengua", "acepta", true, "ciudad", "Mislata"));

        DocumentoPdf documento = DocumentoPdfFactory.getDocumentoPdf(pdf, "prueba.pdf");
        String texto = documento.getPlainText();
        assertTrue(texto.contains("Núria Peñalver l·lengua"), texto);
        assertTrue(texto.contains("Mislata"), texto);
        assertTrue(texto.contains("DOCUMENTO DE PRUEBA"), texto);
        assertFalse(texto.contains("self."), texto);
        assertEquals(List.of(), documento.getNombreCamposFormulario(), "el PDF es plano: sin formulario");
        assertEquals(1, documento.getNumeroPaginas());
    }

    @Test
    void unValorNuloSaleVacio() {
        byte[] pdf = generator.generate(xml("documento_basico.xml"), contexto("nombre", null, "acepta", false, "ciudad", "Mislata"));

        String texto = DocumentoPdfFactory.getDocumentoPdf(pdf, "prueba.pdf").getPlainText();
        assertFalse(texto.contains("null"), texto);
        assertTrue(texto.contains("Mislata"), texto);
    }

    @Test
    void unaFechaSeEstampaConElFormatoDeLaAplicacion() {
        byte[] pdf = generator.generate(xml("documento_basico.xml"), contexto("nombre", LocalDate.of(2026, 9, 24), "acepta", true, "ciudad", ""));

        assertTrue(TextosDelPdf.de(pdf).contiene("24/09/2026"));
    }

    @Test
    void colapsarQuitaElElementoYSubeLoQueVaDetras() {
        TextosDelPdf mostrado = TextosDelPdf.de(generator.generate(xml("documento_colapsar.xml"), contexto("mostrar", true, "nombre", "Ana")));
        TextosDelPdf oculto = TextosDelPdf.de(generator.generate(xml("documento_colapsar.xml"), contexto("mostrar", false, "nombre", "Ana")));

        assertTrue(mostrado.contiene("Oculto"));
        assertFalse(oculto.contiene("Oculto"));
        assertFalse(oculto.contiene("ESCONDIDO"));
        assertFalse(oculto.contiene("CONDICIONAL"));
        assertFalse(oculto.contiene("Contenido"));

        assertTrue(oculto.yDe("Despues") > mostrado.yDe("Despues"), "la línea de detrás del check colapsado sube");
        assertTrue(oculto.yDe("Cierre") > mostrado.yDe("Cierre"), "lo que va detrás de la sección colapsada sube");
        assertEquals(mostrado.yDe("Antes"), oculto.yDe("Antes"), 0.001, "lo de delante no se mueve");
    }

    @Test
    void unaSeccionColapsadaNoConsumeLetra() {
        TextosDelPdf mostrado = TextosDelPdf.de(generator.generate(xml("documento_colapsar.xml"), contexto("mostrar", true, "nombre", "Ana")));
        TextosDelPdf oculto = TextosDelPdf.de(generator.generate(xml("documento_colapsar.xml"), contexto("mostrar", false, "nombre", "Ana")));

        assertTrue(mostrado.contiene("C"), "con tres secciones la última es la C");
        assertFalse(oculto.contiene("C"), "colapsada la B, la última pasa a ser la B");
        assertTrue(oculto.contiene("B"));
    }

    @Test
    void reservarQuitaElElementoPeroDejaSuHueco() {
        TextosDelPdf mostrado = TextosDelPdf.de(generator.generate(xml("documento_reservar.xml"), contexto("mostrar", true, "nombre", "Ana")));
        TextosDelPdf oculto = TextosDelPdf.de(generator.generate(xml("documento_reservar.xml"), contexto("mostrar", false, "nombre", "Ana")));

        assertFalse(oculto.contiene("Oculto"));
        assertFalse(oculto.contiene("ESCONDIDO"));
        assertFalse(oculto.contiene("CONDICIONAL"));
        assertFalse(oculto.contiene("Contenido"));

        assertEquals(mostrado.yDe("Despues"), oculto.yDe("Despues"), 0.001, "el hueco del check se conserva");
        assertEquals(mostrado.yDe("Cierre"), oculto.yDe("Cierre"), 0.001, "el hueco de la sección se conserva");
        assertTrue(oculto.contiene("C"), "una sección reservada sí consume letra");
    }

    @Test
    void unVisibleQueRevientaAbortaLaGeneracion() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> generator.generate(xml("documento_visible_error.xml"), contexto("nombre", "Ana")));

        assertTrue(ex.getMessage().contains("self.noExiste.x"), ex.getMessage());
    }

    @Test
    void unVisibleQueNoDevuelveBooleanAbortaLaGeneracion() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> generator.generate(xml("documento_visible_no_booleano.xml"), contexto("nombre", "Ana")));

        assertTrue(ex.getMessage().contains("self.nombre"), ex.getMessage());
        assertTrue(ex.getMessage().contains("Boolean"), ex.getMessage());
    }

    @Test
    void unCheckCuyoValorNoEsBooleanAbortaLaGeneracion() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> generator.generate(xml("documento_check_no_booleano.xml"), contexto("nombre", "Ana")));

        assertTrue(ex.getMessage().contains("self.nombre"), ex.getMessage());
        assertTrue(ex.getMessage().contains("Boolean"), ex.getMessage());
    }

    @Test
    void unaExpresionDeValorQueRevientaAbortaLaGeneracion() {
        Map<String, Object> sinSelf = new HashMap<>();
        sinSelf.put("self", null);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> generator.generate(xml("documento_basico.xml"), sinSelf));

        assertTrue(ex.getMessage().contains("self.nombre") || ex.getMessage().contains("self.ciudad"), ex.getMessage());
    }

    @Test
    void unValorLargoEnUnCampoHaceCrecerLaFilaYSaleEntero() {
        String largo = "Palabra".repeat(1) + " " + String.join(" ", java.util.Collections.nCopies(60, "observación"));
        TextosDelPdf corto = TextosDelPdf.de(generator.generate(xml("documento_valor_largo.xml"), contexto("texto", "Breve")));
        TextosDelPdf extenso = TextosDelPdf.de(generator.generate(xml("documento_valor_largo.xml"), contexto("texto", largo)));

        assertTrue(extenso.yDe("Siguiente") < corto.yDe("Siguiente"), "la fila del campo crece con el valor");
        long palabras = extenso.todos().stream().filter(t -> t.texto().equals("observación")).count();
        assertEquals(60, palabras, "el valor sale entero, ajustado a líneas");
    }

    // ------------------------------------------------------------------ apoyo

    private static Map<String, Object> contexto(Object... claveValor) {
        Map<String, Object> self = new HashMap<>();
        for (int i = 0; i < claveValor.length; i += 2) {
            self.put((String) claveValor[i], claveValor[i + 1]);
        }
        return Map.of("self", self, "now", LocalDateTime.of(2026, 9, 24, 10, 30));
    }

    private static byte[] xml(String nombre) {
        try (InputStream in = PdfGeneratorImplITextTest.class.getResourceAsStream("../" + nombre)) {
            if (in == null) {
                throw new IllegalStateException("Falta el recurso de test " + nombre);
            }
            return in.readAllBytes();
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
