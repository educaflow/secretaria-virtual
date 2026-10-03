package com.educaflow.base.infrastructure.pdfgenerator.impl;

import com.educaflow.base.infrastructure.pdfgenerator.Idioma;
import com.educaflow.base.infrastructure.pdfgenerator.PdfGenerator;
import com.educaflow.base.infrastructure.pdfgenerator.PdfGeneratorFactory;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Lo que aborta la generación de un documento en prosa, y lo que el mensaje tiene que decir: un
 * documento a medias o con un hueco donde debería ir un dato no vale para nada, así que el
 * generador nunca devuelve un PDF incompleto.
 */
class ErroresDelGeneradorTest {

    private static final String RAIZ_DESCONOCIDA = "<documentoRaro><parrafo/></documentoRaro>";
    private static final String TITULO = "<titulo><castellano>Titulo</castellano></titulo>";
    private static final String NEGRITA_RARA = "<documentoTexto>" + TITULO
            + "<parrafo negrita=\"si\"><castellano>Hola</castellano></parrafo></documentoTexto>";
    private static final String SIN_TITULO =
            "<documentoTexto><parrafo><castellano>Hola</castellano></parrafo></documentoTexto>";

    private final PdfGenerator generator = PdfGeneratorFactory.getPdfGenerator();

    @Test
    void unaVariableQueNoSeDejaEvaluarAbortaLaGeneracion() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> DocumentosDeTexto.pdf("documento_inline_que_revienta.xml", Map.of(), Idioma.CASTELLANO));

        assertTrue(ex.getMessage().contains("self.noExiste.x"),
                "el mensaje no dice qué expresión falló: " + ex.getMessage());
    }

    @Test
    void unVisibleQueNoDevuelveBooleanAbortaLaGeneracion() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> DocumentosDeTexto.pdf("documento_visible_no_booleano.xml", Map.of("nombre", "Ana"), Idioma.CASTELLANO));

        assertTrue(ex.getMessage().contains("self.nombre"), "el mensaje no dice qué expresión falló: " + ex.getMessage());
        assertTrue(ex.getMessage().contains("Boolean"), "el mensaje no dice qué se esperaba: " + ex.getMessage());
        assertTrue(ex.getMessage().contains(String.class.getName()), "el mensaje no dice qué tipo devolvió: " + ex.getMessage());
        assertFalse(ex.getMessage().contains("Ana"), "el mensaje no debe incluir el valor evaluado: " + ex.getMessage());
    }

    @Test
    void unAtributoBooleanoQueNoEsTrueNiFalseAbortaLaGeneracion() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> DocumentosDeTexto.pdfDe(NEGRITA_RARA, Map.of(), Idioma.CASTELLANO));

        assertTrue(ex.getMessage().contains("negrita"), "el mensaje no dice qué atributo falló: " + ex.getMessage());
        assertTrue(ex.getMessage().contains("\"si\""), "el mensaje no dice qué valor traía: " + ex.getMessage());
        assertTrue(ex.getMessage().contains("true") && ex.getMessage().contains("false"),
                "el mensaje no dice qué se admite: " + ex.getMessage());
    }

    @Test
    void unDocumentoSinTituloAbortaLaGeneracion() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> DocumentosDeTexto.pdfDe(SIN_TITULO, Map.of(), Idioma.CASTELLANO));

        assertTrue(ex.getMessage().contains("titulo"), "el mensaje no dice qué elemento falta: " + ex.getMessage());
        assertTrue(ex.getMessage().contains("documentoTexto"), "el mensaje no dice dónde falta: " + ex.getMessage());
    }

    @Test
    void unaRaizQueNoEsDeNingunTipoDeDocumentoAbortaLaGeneracion() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> generator.generate(raizDesconocida(), Map.of(), Idioma.CASTELLANO));

        assertTrue(ex.getMessage().contains("documentoRaro"), "el mensaje no dice qué raíz trae el XML: " + ex.getMessage());
        assertTrue(ex.getMessage().contains("documentoFormulario") && ex.getMessage().contains("documentoTexto"),
                "el mensaje no dice qué raíces se admiten: " + ex.getMessage());
    }

    @Test
    void unaRaizQueNoEsDeNingunTipoDeDocumentoAbortaTambienElListadoDeExpresiones() {
        RuntimeException ex = assertThrows(RuntimeException.class, () -> generator.getExpresiones(raizDesconocida()));

        assertTrue(ex.getMessage().contains("documentoRaro"), "el mensaje no dice qué raíz trae el XML: " + ex.getMessage());
    }

    private static byte[] raizDesconocida() {
        return RAIZ_DESCONOCIDA.getBytes(StandardCharsets.UTF_8);
    }
}
