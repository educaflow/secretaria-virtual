package com.educaflow.base.infrastructure.pdfgenerator.impl.comun.dibujo;

import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.texto.Fuente;
import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.texto.Linea;
import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.texto.Parrafo;
import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.texto.TipoToken;
import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.texto.Token;
import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.texto.TokenColocado;
import com.itextpdf.forms.PdfAcroForm;
import com.itextpdf.forms.fields.SignatureFormFieldBuilder;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;

/**
 * Las primitivas de dibujo sobre las páginas del PDF: líneas, rectángulos, texto, párrafos, imágenes,
 * casillas y campos de firma. Lleva el cursor vertical (el borde superior libre, en coordenadas PDF) y abre página
 * nueva cuando lo que viene no cabe en la banda que le deja el {@link MarcoPagina}.
 */
public final class Lienzo {

    private final PdfDocument pdf;
    private final Fuentes fuentes;
    private final MarcoPagina marco;
    private PdfPage pagina;
    private PdfCanvas canvas;
    private double cursorY;

    public Lienzo(PdfDocument pdf, Fuentes fuentes, MarcoPagina marco) {
        this.pdf = pdf;
        this.fuentes = fuentes;
        this.marco = marco;
        nuevaPagina();
    }

    void nuevaPagina() {
        pagina = pdf.addNewPage(new PageSize((float) MedidasPagina.PAGE_W, (float) MedidasPagina.PAGE_H));
        canvas = new PdfCanvas(pagina);
        cursorY = marco.topCuerpo();
        marco.dibujo().accept(this);
    }

    public void asegurarEspacio(double alto) {
        if (cursorY - alto < marco.bottomCuerpo()) {
            nuevaPagina();
        }
    }

    public double cursorY() {
        return cursorY;
    }

    public void avanzar(double alto) {
        cursorY -= alto;
    }

    void trazar(double x1, double y1, double x2, double y2) {
        canvas.saveState().setLineWidth(0.5f).setStrokeColorGray(0f)
                .moveTo(x1, y1).lineTo(x2, y2).stroke().restoreState();
    }

    public void rellenarGris(double x, double y, double ancho, double alto, double gris) {
        canvas.saveState().setFillColorGray((float) gris).rectangle(x, y, ancho, alto).fill().restoreState();
    }

    public void bordesCelda(double x, double yTop, double ancho, double alto, boolean superior, boolean inferior) {
        trazar(x, yTop - alto, x, yTop);
        trazar(x + ancho, yTop - alto, x + ancho, yTop);
        if (superior) {
            trazar(x, yTop, x + ancho, yTop);
        }
        if (inferior) {
            trazar(x, yTop - alto, x + ancho, yTop - alto);
        }
    }

    public void texto(Fuente fuente, double tamanyo, double x, double y, String texto) {
        canvas.beginText().setFontAndSize(fuentes.de(fuente), (float) tamanyo)
                .moveText(x, y).showText(texto).endText();
    }

    /** Dibuja un párrafo desde su borde superior; la línea base va al 82 % del alto de línea. */
    public void parrafo(Parrafo parrafo, double x, double yTop, double anchoMaximo, Alineacion alineacion) {
        double y = yTop;
        for (Linea linea : parrafo.lineas()) {
            linea(linea, x, y - parrafo.altoLinea() * 0.82, anchoMaximo, alineacion);
            y -= parrafo.altoLinea();
        }
    }

    private void linea(Linea linea, double x, double base, double anchoMaximo, Alineacion alineacion) {
        double dx = desplazamiento(linea, anchoMaximo, alineacion);
        double ensanche = ensancheDeEspacio(linea, anchoMaximo, alineacion);
        int espacios = 0;
        // los espacios también se estampan: sin ellos, al extraer el texto del PDF las palabras salen pegadas
        for (TokenColocado colocado : linea.tokens()) {
            Token token = colocado.token();
            texto(token.fuente(), token.tamanyo(), x + dx + colocado.x() + espacios * ensanche, base, token.texto());
            if (token.tipo() == TipoToken.ESPACIO) {
                espacios++;
            }
        }
    }

    private static double desplazamiento(Linea linea, double anchoMaximo, Alineacion alineacion) {
        return switch (alineacion) {
            case IZQUIERDA, JUSTIFICADO -> 0;
            case CENTRO -> (anchoMaximo - linea.ancho()) / 2;
            case DERECHA -> anchoMaximo - linea.anchoSinEspaciosFinales();
        };
    }

    /** Lo que se le suma a cada espacio para que la línea llegue al borde derecho. */
    private static double ensancheDeEspacio(Linea linea, double anchoMaximo, Alineacion alineacion) {
        if (alineacion != Alineacion.JUSTIFICADO || linea.finalDeParrafo() || linea.espaciosInteriores() == 0) {
            return 0;
        }
        return Math.max(0, anchoMaximo - linea.anchoSinEspaciosFinales()) / linea.espaciosInteriores();
    }

    public void imagen(ImageData imagen, double x, double y, double ancho, double alto) {
        canvas.addImageFittedIntoRectangle(imagen, new Rectangle((float) x, (float) y, (float) ancho, (float) alto), false);
    }

    public void logo(double x, double y, double ancho, double alto) {
        imagen(Assets.logo(), x, y, ancho, alto);
    }

    /**
     * Un campo de firma vacío en la página actual: no dibuja nada, deja en el PDF el sitio (página y
     * recuadro) en el que firmará quien lo haga indicando este nombre.
     */
    public void campoFirma(String nombre, double x, double y, double ancho, double alto) {
        PdfAcroForm formulario = PdfAcroForm.getAcroForm(pdf, true);
        if (formulario.getField(nombre) != null) {
            throw new RuntimeException("El documento tiene dos campos de firma que se llaman \"" + nombre + "\":"
                    + " el nombre de un campoFirma no se puede repetir");
        }
        formulario.addField(new SignatureFormFieldBuilder(pdf, nombre)
                .setWidgetRectangle(new Rectangle((float) x, (float) y, (float) ancho, (float) alto))
                .setPage(pagina)
                .createSignature(), pagina);
    }

    /** La casilla de un check: recuadro y, si va marcada, un aspa. Mismos trazos que la casilla de siempre. */
    public void casilla(double x, double y, double lado, boolean marcada) {
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
