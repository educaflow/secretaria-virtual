package com.educaflow.base.infrastructure.pdfgenerator.impl;

import com.educaflow.base.infrastructure.pdf.DocumentoPdf;
import com.educaflow.base.infrastructure.pdf.DocumentoPdfFactory;
import com.educaflow.base.infrastructure.pdfgenerator.Idioma;
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

    /** Las filas {@code <campo>} de {@code documento_salto_pagina.xml}. */
    private static final int FILAS_DEL_SALTO = 35;

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
        byte[] pdf = generator.generate(xml("documento_basico.xml"), contexto("nombre", "Núria Peñalver l·lengua", "acepta", true, "ciudad", "Mislata"), Idioma.CASTELLANO);

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
        byte[] pdf = generator.generate(xml("documento_basico.xml"), contexto("nombre", null, "acepta", false, "ciudad", "Mislata"), Idioma.CASTELLANO);

        String texto = DocumentoPdfFactory.getDocumentoPdf(pdf, "prueba.pdf").getPlainText();
        assertFalse(texto.contains("null"), texto);
        assertTrue(texto.contains("Mislata"), texto);
    }

    @Test
    void unaFechaSeEstampaConElFormatoDeLaAplicacion() {
        byte[] pdf = generator.generate(xml("documento_basico.xml"), contexto("nombre", LocalDate.of(2026, 9, 24), "acepta", true, "ciudad", ""), Idioma.CASTELLANO);

        assertTrue(TextosDelPdf.de(pdf).contiene("24/09/2026"));
    }

    @Test
    void colapsarQuitaElElementoYSubeLoQueVaDetras() {
        TextosDelPdf mostrado = TextosDelPdf.de(generator.generate(xml("documento_colapsar.xml"), contexto("mostrar", true, "nombre", "Ana"), Idioma.CASTELLANO));
        TextosDelPdf oculto = TextosDelPdf.de(generator.generate(xml("documento_colapsar.xml"), contexto("mostrar", false, "nombre", "Ana"), Idioma.CASTELLANO));

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
        TextosDelPdf mostrado = TextosDelPdf.de(generator.generate(xml("documento_colapsar.xml"), contexto("mostrar", true, "nombre", "Ana"), Idioma.CASTELLANO));
        TextosDelPdf oculto = TextosDelPdf.de(generator.generate(xml("documento_colapsar.xml"), contexto("mostrar", false, "nombre", "Ana"), Idioma.CASTELLANO));

        assertTrue(mostrado.contiene("C"), "con tres secciones la última es la C");
        assertFalse(oculto.contiene("C"), "colapsada la B, la última pasa a ser la B");
        assertTrue(oculto.contiene("B"));
    }

    @Test
    void reservarQuitaElElementoPeroDejaSuHueco() {
        TextosDelPdf mostrado = TextosDelPdf.de(generator.generate(xml("documento_reservar.xml"), contexto("mostrar", true, "nombre", "Ana"), Idioma.CASTELLANO));
        TextosDelPdf oculto = TextosDelPdf.de(generator.generate(xml("documento_reservar.xml"), contexto("mostrar", false, "nombre", "Ana"), Idioma.CASTELLANO));

        assertFalse(oculto.contiene("Oculto"));
        assertFalse(oculto.contiene("ESCONDIDO"));
        assertFalse(oculto.contiene("CONDICIONAL"));
        assertFalse(oculto.contiene("Contenido"));

        assertEquals(mostrado.yDe("Despues"), oculto.yDe("Despues"), 0.001, "el hueco del check se conserva");
        assertEquals(mostrado.yDe("Cierre"), oculto.yDe("Cierre"), 0.001, "el hueco de la sección se conserva");
        assertTrue(oculto.contiene("C"), "una sección reservada sí consume letra");
    }

    @Test
    void unaSeccionReservadaNoEvaluaLosInlineDeSuTitulo() {
        TextosDelPdf oculto = TextosDelPdf.de(generator.generate(xml("documento_reservar_titulo_inline.xml"), contexto("mostrar", false, "nombre", "Ana"), Idioma.CASTELLANO));

        assertFalse(oculto.contiene("CONDICIONAL"));
        assertTrue(oculto.contiene("Cierre"));
    }

    @Test
    void unVisibleQueRevientaAbortaLaGeneracion() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> generator.generate(xml("documento_visible_error.xml"), contexto("nombre", "Ana"), Idioma.CASTELLANO));

        assertTrue(ex.getMessage().contains("self.noExiste.x"), ex.getMessage());
    }

    @Test
    void unVisibleQueNoDevuelveBooleanAbortaLaGeneracion() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> generator.generate(xml("documento_visible_no_booleano.xml"), contexto("nombre", "Ana"), Idioma.CASTELLANO));

        assertTrue(ex.getMessage().contains("self.nombre"), ex.getMessage());
        assertTrue(ex.getMessage().contains("Boolean"), ex.getMessage());
    }

    @Test
    void unCheckCuyoValorNoEsBooleanAbortaLaGeneracion() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> generator.generate(xml("documento_check_no_booleano.xml"), contexto("nombre", "Ana"), Idioma.CASTELLANO));

        assertTrue(ex.getMessage().contains("self.nombre"), ex.getMessage());
        assertTrue(ex.getMessage().contains("Boolean"), ex.getMessage());
    }

    @Test
    void unaExpresionDeValorQueRevientaAbortaLaGeneracion() {
        Map<String, Object> sinSelf = new HashMap<>();
        sinSelf.put("self", null);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> generator.generate(xml("documento_basico.xml"), sinSelf, Idioma.CASTELLANO));

        assertTrue(ex.getMessage().contains("self.nombre") || ex.getMessage().contains("self.ciudad"), ex.getMessage());
    }

    @Test
    void unValorLargoEnUnCampoHaceCrecerLaFilaYSaleEntero() {
        String largo = "Palabra".repeat(1) + " " + String.join(" ", java.util.Collections.nCopies(60, "observación"));
        TextosDelPdf corto = TextosDelPdf.de(generator.generate(xml("documento_valor_largo.xml"), contexto("texto", "Breve"), Idioma.CASTELLANO));
        TextosDelPdf extenso = TextosDelPdf.de(generator.generate(xml("documento_valor_largo.xml"), contexto("texto", largo), Idioma.CASTELLANO));

        assertTrue(extenso.yDe("Siguiente") < corto.yDe("Siguiente"), "la fila del campo crece con el valor");
        long palabras = extenso.todos().stream().filter(t -> t.texto().equals("observación")).count();
        assertEquals(60, palabras, "el valor sale entero, ajustado a líneas");
    }

    @Test
    void getExpresionesIncluyeLaDelTituloYLaPoneLaPrimera() {
        List<String> expresiones = generator.getExpresiones(xml("documento_titulo_inline.xml"));

        assertEquals(List.of("self.titular", "self.mostrar", "self.ciudad"), expresiones);
    }

    @Test
    void loQueNoCabeEnLaPrimeraPaginaSigueEnLaSegunda() {
        byte[] pdf = generator.generate(xml("documento_salto_pagina.xml"), contexto("texto", "Valor"), Idioma.CASTELLANO);

        assertEquals(2, DocumentoPdfFactory.getDocumentoPdf(pdf, "prueba.pdf").getNumeroPaginas());
        List<TextosDelPdf.Texto> textos = TextosDelPdf.de(pdf).todos();
        assertEquals(2, textos.stream().filter(t -> t.texto().equals("Ultima")).findFirst().orElseThrow().pagina(),
                "la última fila cae ya en la segunda página");
        assertEquals(FILAS_DEL_SALTO, textos.stream().filter(t -> t.texto().equals("Valor")).count(),
                "ninguna fila se pierde ni se repite en el corte");
    }

    @Test
    void unInlineCuyoValorEvaluaAVacioNoOcupaNada() {
        double sinInline = TextosDelPdf.de(generator.generate(xml("documento_inline_hueco_sin_inline.xml"), contexto(), Idioma.CASTELLANO)).xDe("Despues");
        double conValorCorto = TextosDelPdf.de(generator.generate(xml("documento_inline_hueco.xml"), contexto("dato", "X"), Idioma.CASTELLANO)).xDe("Despues");
        double conValorLargo = TextosDelPdf.de(generator.generate(xml("documento_inline_hueco.xml"),
                contexto("dato", "Un valor inline bastante mas largo que el corto"), Idioma.CASTELLANO)).xDe("Despues");
        double vacio = TextosDelPdf.de(generator.generate(xml("documento_inline_hueco.xml"), contexto("dato", null), Idioma.CASTELLANO)).xDe("Despues");

        assertEquals(sinInline, vacio, 0.01, "con el valor vacío el inline no desplaza lo que va detrás");
        assertTrue(conValorCorto > sinInline, "un valor con texto desplaza lo que va detrás lo que mide");
        assertTrue(conValorLargo > conValorCorto, "y cuanto más ancho es el valor, más lo desplaza");
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
