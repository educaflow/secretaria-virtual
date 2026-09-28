package com.educaflow.base.infrastructure.pdfgenerator.impl;

import com.itextpdf.kernel.geom.Matrix;
import com.itextpdf.kernel.geom.Vector;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.EventType;
import com.itextpdf.kernel.pdf.canvas.parser.PdfCanvasProcessor;
import com.itextpdf.kernel.pdf.canvas.parser.data.IEventData;
import com.itextpdf.kernel.pdf.canvas.parser.data.ImageRenderInfo;
import com.itextpdf.kernel.pdf.canvas.parser.data.TextRenderInfo;
import com.itextpdf.kernel.pdf.canvas.parser.listener.IEventListener;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Apoyo de los tests: lo que el generador estampó en un PDF. Los textos, con la x de inicio y la de
 * fin de su línea base; las imágenes, con el rectángulo que ocupan; y cuántos trazos de camino
 * (líneas, recuadros, rellenos) se dibujaron. El generador dibuja cada token (palabra, espacio,
 * valor) con una operación de texto propia, así que cada palabra suelta es un texto que se puede
 * buscar por igualdad.
 */
final class TextosDelPdf implements IEventListener {

    record Texto(String texto, double x, double xFin, double y, int pagina) {
    }

    /**
     * Los tokens que comparten línea base en una misma página, en el orden en que se estamparon.
     * Los bordes son los del primer y el último token con tinta: un espacio final se estampa más
     * allá del borde derecho de la caja, y no cuenta como parte de la línea.
     */
    record Linea(int pagina, double y, double xInicio, double xFin, String texto) {
    }

    /** El rectángulo que ocupa una imagen estampada. */
    record Imagen(int pagina, double x, double y, double ancho, double alto) {
    }

    private final List<Texto> textos = new ArrayList<>();
    private final List<Imagen> imagenes = new ArrayList<>();
    private int trazos;
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
        switch (type) {
            case RENDER_TEXT -> anyadirTexto((TextRenderInfo) data);
            case RENDER_IMAGE -> anyadirImagen((ImageRenderInfo) data);
            case RENDER_PATH -> trazos++;
            default -> { }
        }
    }

    private void anyadirTexto(TextRenderInfo info) {
        Vector inicio = info.getBaseline().getStartPoint();
        Vector fin = info.getBaseline().getEndPoint();
        textos.add(new Texto(info.getText(), inicio.get(Vector.I1), fin.get(Vector.I1), inicio.get(Vector.I2), pagina));
    }

    private void anyadirImagen(ImageRenderInfo info) {
        Matrix ctm = info.getImageCtm();
        imagenes.add(new Imagen(pagina, ctm.get(Matrix.I31), ctm.get(Matrix.I32),
                ctm.get(Matrix.I11), ctm.get(Matrix.I22)));
    }

    @Override
    public Set<EventType> getSupportedEvents() {
        return Set.of(EventType.RENDER_TEXT, EventType.RENDER_IMAGE, EventType.RENDER_PATH);
    }

    boolean contiene(String texto) {
        return textos.stream().anyMatch(t -> t.texto().equals(texto));
    }

    /** La y de la línea base del único texto igual al dado. */
    double yDe(String texto) {
        return unico(texto).y();
    }

    /** La x de inicio de la línea base del único texto igual al dado. */
    double xDe(String texto) {
        return unico(texto).x();
    }

    private Texto unico(String texto) {
        List<Texto> iguales = textos.stream().filter(t -> t.texto().equals(texto)).toList();
        if (iguales.size() != 1) {
            throw new AssertionError("Se esperaba exactamente un texto «" + texto + "» y hay " + iguales.size() + ": " + textos);
        }
        return iguales.get(0);
    }

    List<Texto> todos() {
        return textos;
    }

    List<Imagen> imagenes() {
        return imagenes;
    }

    /** Cuántos caminos se pintaron: las líneas, los recuadros y los rellenos. */
    int trazos() {
        return trazos;
    }

    /** Las líneas estampadas, de arriba abajo dentro de cada página. */
    List<Linea> lineas() {
        List<Linea> lineas = new ArrayList<>();
        for (Texto texto : textos) {
            int ultima = lineas.size() - 1;
            if (ultima >= 0 && esDeLaLinea(lineas.get(ultima), texto)) {
                lineas.set(ultima, con(lineas.get(ultima), texto));
            } else {
                lineas.add(con(new Linea(texto.pagina(), texto.y(), Double.NaN, Double.NaN, ""), texto));
            }
        }
        return lineas;
    }

    private static boolean esDeLaLinea(Linea linea, Texto texto) {
        return linea.pagina() == texto.pagina() && Math.abs(linea.y() - texto.y()) < 0.01;
    }

    private static Linea con(Linea linea, Texto texto) {
        String contenido = linea.texto() + texto.texto();
        if (texto.texto().isBlank()) {
            return new Linea(linea.pagina(), linea.y(), linea.xInicio(), linea.xFin(), contenido);
        }
        double inicio = Double.isNaN(linea.xInicio()) ? texto.x() : Math.min(linea.xInicio(), texto.x());
        double fin = Double.isNaN(linea.xFin()) ? texto.xFin() : Math.max(linea.xFin(), texto.xFin());
        return new Linea(linea.pagina(), linea.y(), inicio, fin, contenido);
    }

    /** En el orden en que iText emitió los eventos. */
    List<String> volcado() {
        return textos.stream()
                .map(t -> String.format(Locale.ROOT, "%d|%.2f|%.2f|%s", t.pagina(), t.x(), t.y(), t.texto()))
                .toList();
    }
}
