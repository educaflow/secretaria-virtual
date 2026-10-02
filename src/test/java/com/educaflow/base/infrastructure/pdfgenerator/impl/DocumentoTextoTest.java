package com.educaflow.base.infrastructure.pdfgenerator.impl;

import com.educaflow.base.infrastructure.pdfgenerator.Idioma;
import com.educaflow.base.infrastructure.pdfgenerator.PdfGenerator;
import com.educaflow.base.infrastructure.pdfgenerator.PdfGeneratorFactory;
import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.dibujo.MedidasPagina;
import com.itextpdf.kernel.pdf.PdfDictionary;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfName;
import com.itextpdf.kernel.pdf.PdfReader;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentoTextoTest {

    private static final String CARPETA = "/com/educaflow/base/infrastructure/pdfgenerator/documentotexto/";
    private static final Path DESTINO = Path.of("build/documentotexto");

    /** El alto de la página menos su margen, tomados del generador: sin pie, el cuerpo llega hasta abajo. */
    private static final double TOP_PAGINA = MedidasPagina.PAGE_H - MedidasPagina.MARGIN;
    private static final double BOTTOM_PAGINA = MedidasPagina.MARGIN;

    private static final Map<String, Object> SELF = Map.of("nombre", "Marta Gonzalez Lorenzo", "mostrar", true);

    private final PdfGenerator generator = PdfGeneratorFactory.getPdfGenerator();

    @Test
    void elDocumentoEnProsaEstampaTodosSusElementosDeArribaAbajo() {
        TextosDelPdf textos = TextosDelPdf.de(generar("documento_prosa.xml", Idioma.CASTELLANO));

        assertTrue(textos.contiene("CERTIFICADO"), "No está el título: " + textos.volcado());
        assertTrue(textos.yDe("CERTIFICADO") > textos.yDe("certifica"), "El título no va encima del párrafo");
        assertTrue(textos.yDe("certifica") > textos.yDe("Docencia"), "El párrafo no va encima de la lista");
        assertTrue(textos.yDe("Docencia") > textos.yDe("Tutoria"), "Los items de la lista no van en orden");
        assertTrue(textos.yDe("Tutoria") > textos.yDe("CONCEPTO"), "La lista no va encima de la tabla");
        assertTrue(textos.yDe("CONCEPTO") > textos.yDe("Lectivas"), "Las filas de la tabla no van en orden");

        assertEquals(3, textos.todos().stream().filter(t -> t.texto().equals("•")).count(),
                "No hay una viñeta por item: " + textos.volcado());
        assertEquals(textos.yDe("CONCEPTO"), textos.yDe("HORAS"),
                "Las dos celdas de una fila no van a la misma altura");
        assertTrue(textos.xDe("HORAS") > textos.xDe("CONCEPTO") + 200,
                "La segunda columna no está alineada a la derecha de su celda");
    }

    @Test
    void lasExpresionesSonLasDeLosDosIdiomasSinDuplicados() {
        assertEquals(List.of("self.nombre"), generator.getExpresiones(xml("documento_prosa.xml")));
    }

    @Test
    void elCuerpoSaltaDePaginaPorLineasYLaCabeceraSoloVaEnLaPrimera() {
        byte[] pdf = generar("documento_dos_paginas.xml", Idioma.CASTELLANO);
        TextosDelPdf textos = TextosDelPdf.de(pdf);

        assertEquals(2, paginas(pdf), "El documento no ocupa dos páginas");
        assertEquals(1, imagenesDe(pdf, 1), "La primera página no lleva el logo de la cabecera");
        assertEquals(0, imagenesDe(pdf, 2), "La segunda página lleva cabecera: solo la primera debe llevarla");
        assertEquals(1, paginaDe(textos, "ALFA"), "El párrafo largo no empieza en la primera página");
        assertEquals(2, paginaDe(textos, "OMEGA"), "El párrafo largo no siguió en la segunda: no se partió por líneas");
        assertEquals(2, paginaDe(textos, "ULTIMA"), "El último título no cayó en la segunda página");

        textos.todos().forEach(texto -> assertTrue(texto.y() > BOTTOM_PAGINA && texto.y() < TOP_PAGINA,
                "El texto «" + texto.texto() + "» se sale de la página: y=" + texto.y()));
    }

    @Test
    void cadaIdiomaSeEmiteSolo() {
        TextosDelPdf castellano = TextosDelPdf.de(generar("documento_idiomas.xml", Idioma.CASTELLANO));
        TextosDelPdf valenciano = TextosDelPdf.de(generar("documento_idiomas.xml", Idioma.VALENCIANO));

        assertTrue(castellano.contiene("castellano."), "Falta el castellano: " + castellano.volcado());
        assertFalse(castellano.contiene("valencia."), "Sale el valenciano: " + castellano.volcado());
        assertTrue(valenciano.contiene("valencia."), "Falta el valenciano: " + valenciano.volcado());
        assertFalse(valenciano.contiene("castellano."), "Sale el castellano: " + valenciano.volcado());
        assertTrue(castellano.contiene("DOCUMENTO"), "El título no salió en castellano: " + castellano.volcado());
        assertTrue(valenciano.contiene("DOCUMENT"), "El título no salió en valenciano: " + valenciano.volcado());
    }

    @Test
    void colapsarQuitaElBloqueYReservarLeDejaSuHueco() {
        String xml = new String(xml("documento_visibilidad.xml"), StandardCharsets.UTF_8);
        TextosDelPdf visible = generar("visible", xml, SELF);
        TextosDelPdf colapsado = generar("colapsado", xml, Map.of("mostrar", false));
        TextosDelPdf reservado = generar("reservado",
                xml.replace("visible=\"self.mostrar\"", "visible=\"self.mostrar\" siOculto=\"reservar\""),
                Map.of("mostrar", false));

        assertTrue(visible.contiene("Condicional"), "No sale el párrafo condicional cuando su visible es true");
        assertFalse(colapsado.contiene("Condicional"), "Sale el párrafo colapsado");
        assertFalse(reservado.contiene("Condicional"), "Sale el párrafo reservado");
        assertTrue(colapsado.yDe("Cierre") > visible.yDe("Cierre"), "Al colapsar no subió lo que va debajo");
        assertEquals(visible.yDe("Cierre"), reservado.yDe("Cierre"), 0.01, "Al reservar se movió lo que va debajo");
    }

    // ------------------------------------------------------------------ apoyo

    private static int paginaDe(TextosDelPdf textos, String texto) {
        return textos.todos().stream().filter(t -> t.texto().equals(texto)).findFirst().orElseThrow().pagina();
    }

    private byte[] generar(String documento, Idioma idioma) {
        return guardar(idioma + "-" + documento, generator.generate(xml(documento), Map.of("self", SELF), idioma));
    }

    private TextosDelPdf generar(String etiqueta, String xml, Map<String, Object> self) {
        byte[] pdf = generator.generate(xml.getBytes(StandardCharsets.UTF_8), Map.of("self", self), Idioma.CASTELLANO);
        return TextosDelPdf.de(guardar(etiqueta + "-documento_visibilidad.xml", pdf));
    }

    /** Deja el PDF en {@link #DESTINO} para poder rasterizarlo y mirarlo. */
    private static byte[] guardar(String nombre, byte[] pdf) {
        try {
            Files.write(Files.createDirectories(DESTINO).resolve(nombre + ".pdf"), pdf);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
        return pdf;
    }

    private static byte[] xml(String documento) {
        try (InputStream in = DocumentoTextoTest.class.getResourceAsStream(CARPETA + documento)) {
            return in.readAllBytes();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private static int paginas(byte[] pdf) {
        try (PdfDocument documento = abrir(pdf)) {
            return documento.getNumberOfPages();
        }
    }

    private static int imagenesDe(byte[] pdf, int pagina) {
        try (PdfDocument documento = abrir(pdf)) {
            PdfDictionary xobjects = documento.getPage(pagina).getResources().getPdfObject().getAsDictionary(PdfName.XObject);
            return xobjects == null ? 0 : xobjects.size();
        }
    }

    private static PdfDocument abrir(byte[] pdf) {
        try {
            return new PdfDocument(new PdfReader(new ByteArrayInputStream(pdf)));
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
