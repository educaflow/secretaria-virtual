package com.educaflow.base.infrastructure.pdfgenerator.impl.dibujo;

import com.educaflow.base.infrastructure.pdfgenerator.impl.maquetacion.Medidas;
import com.educaflow.base.infrastructure.pdfgenerator.impl.texto.Fuente;
import com.educaflow.base.infrastructure.pdfgenerator.impl.texto.Linea;
import com.educaflow.base.infrastructure.pdfgenerator.impl.texto.Parrafo;
import com.educaflow.base.infrastructure.pdfgenerator.impl.texto.Token;
import com.educaflow.base.infrastructure.pdfgenerator.impl.texto.TokenColocado;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;

/**
 * Las primitivas de dibujo sobre las páginas del PDF: líneas, rectángulos, texto, párrafos, el logo
 * y las casillas. Lleva el cursor vertical (el borde superior libre, en coordenadas PDF) y abre
 * página nueva cuando lo que viene no cabe.
 */
final class Lienzo {

    private final PdfDocument pdf;
    private final Fuentes fuentes;
    private PdfCanvas canvas;
    private double cursorY;

    Lienzo(PdfDocument pdf, Fuentes fuentes) {
        this.pdf = pdf;
        this.fuentes = fuentes;
        nuevaPagina();
    }

    void nuevaPagina() {
        PdfPage pagina = pdf.addNewPage(new PageSize((float) Medidas.PAGE_W, (float) Medidas.PAGE_H));
        canvas = new PdfCanvas(pagina);
        cursorY = Medidas.PAGE_H - Medidas.MARGIN;
    }

    void asegurarEspacio(double alto) {
        if (cursorY - alto < Medidas.MARGIN) {
            nuevaPagina();
        }
    }

    double cursorY() {
        return cursorY;
    }

    void avanzar(double alto) {
        cursorY -= alto;
    }

    void trazar(double x1, double y1, double x2, double y2) {
        canvas.saveState().setLineWidth(0.5f).setStrokeColorGray(0f)
                .moveTo(x1, y1).lineTo(x2, y2).stroke().restoreState();
    }

    void rellenarGris(double x, double y, double ancho, double alto, double gris) {
        canvas.saveState().setFillColorGray((float) gris).rectangle(x, y, ancho, alto).fill().restoreState();
    }

    void bordesCelda(double x, double yTop, double ancho, double alto, boolean superior, boolean inferior) {
        trazar(x, yTop - alto, x, yTop);
        trazar(x + ancho, yTop - alto, x + ancho, yTop);
        if (superior) {
            trazar(x, yTop, x + ancho, yTop);
        }
        if (inferior) {
            trazar(x, yTop - alto, x + ancho, yTop - alto);
        }
    }

    void texto(Fuente fuente, double tamanyo, double x, double y, String texto) {
        canvas.beginText().setFontAndSize(fuentes.de(fuente), (float) tamanyo)
                .moveText(x, y).showText(texto).endText();
    }

    /** Dibuja un párrafo desde su borde superior; la línea base va al 82 % del alto de línea. */
    void parrafo(Parrafo parrafo, double x, double yTop, double anchoMaximo, Alineacion alineacion) {
        double y = yTop;
        for (Linea linea : parrafo.lineas()) {
            double base = y - parrafo.altoLinea() * 0.82;
            double dx = alineacion == Alineacion.CENTRO ? (anchoMaximo - linea.ancho()) / 2 : 0;
            // los espacios también se estampan: sin ellos, al extraer el texto del PDF las palabras salen pegadas
            for (TokenColocado colocado : linea.tokens()) {
                Token token = colocado.token();
                texto(token.fuente(), token.tamanyo(), x + dx + colocado.x(), base, token.texto());
            }
            y -= parrafo.altoLinea();
        }
    }

    void logo(double x, double y, double ancho, double alto) {
        canvas.addImageFittedIntoRectangle(FuentesRoboto.logo(), new Rectangle((float) x, (float) y, (float) ancho, (float) alto), false);
    }

    /** La casilla de un check: recuadro y, si va marcada, un aspa. Mismos trazos que la casilla de siempre. */
    void casilla(double x, double y, double lado, boolean marcada) {
        canvas.saveState().setLineWidth(0.6f).setStrokeColorGray(0f)
                .rectangle(x + 0.3, y + 0.3, lado - 0.6, lado - 0.6).stroke().restoreState();
        if (marcada) {
            canvas.saveState().setLineWidth(0.9f).setStrokeColorGray(0f)
                    .moveTo(x + 1.6, y + 1.6).lineTo(x + lado - 1.6, y + lado - 1.6).stroke()
                    .moveTo(x + lado - 1.6, y + 1.6).lineTo(x + 1.6, y + lado - 1.6).stroke()
                    .restoreState();
        }
    }
}
