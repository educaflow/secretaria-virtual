package com.educaflow.base.infrastructure.pdfgenerator.impl;

import com.itextpdf.kernel.geom.Vector;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.EventType;
import com.itextpdf.kernel.pdf.canvas.parser.PdfCanvasProcessor;
import com.itextpdf.kernel.pdf.canvas.parser.data.IEventData;
import com.itextpdf.kernel.pdf.canvas.parser.data.TextRenderInfo;
import com.itextpdf.kernel.pdf.canvas.parser.listener.IEventListener;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Apoyo de los tests: los textos que el generador estampó en un PDF, con la y de su línea base. El
 * generador dibuja cada token (palabra, valor) con una operación de texto propia, así que cada
 * palabra suelta es un texto que se puede buscar por igualdad.
 */
final class TextosDelPdf implements IEventListener {

    record Texto(String texto, double y, int pagina) {
    }

    private final List<Texto> textos = new ArrayList<>();
    private int pagina;

    static TextosDelPdf de(byte[] pdf) {
        try (PdfDocument documento = new PdfDocument(new PdfReader(new ByteArrayInputStream(pdf)))) {
            TextosDelPdf textos = new TextosDelPdf();
            PdfCanvasProcessor procesador = new PdfCanvasProcessor(textos);
            for (int i = 1; i <= documento.getNumberOfPages(); i++) {
                textos.pagina = i;
                procesador.processPageContent(documento.getPage(i));
            }
            return textos;
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Override
    public void eventOccurred(IEventData data, EventType type) {
        if (type == EventType.RENDER_TEXT) {
            TextRenderInfo info = (TextRenderInfo) data;
            textos.add(new Texto(info.getText(), info.getBaseline().getStartPoint().get(Vector.I2), pagina));
        }
    }

    @Override
    public Set<EventType> getSupportedEvents() {
        return Set.of(EventType.RENDER_TEXT);
    }

    boolean contiene(String texto) {
        return textos.stream().anyMatch(t -> t.texto().equals(texto));
    }

    /** La y de la línea base del único texto igual al dado. */
    double yDe(String texto) {
        List<Texto> iguales = textos.stream().filter(t -> t.texto().equals(texto)).toList();
        if (iguales.size() != 1) {
            throw new AssertionError("Se esperaba exactamente un texto «" + texto + "» y hay " + iguales.size() + ": " + textos);
        }
        return iguales.get(0).y();
    }

    List<Texto> todos() {
        return textos;
    }
}
