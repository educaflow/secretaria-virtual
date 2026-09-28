package com.educaflow.base.infrastructure.pdfgenerator.impl.comun.dibujo;

import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.texto.Familia;
import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.texto.MedidorTexto;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;

import java.io.ByteArrayOutputStream;
import java.util.function.BiConsumer;

/** Abre un PDF con las fuentes del proyecto y devuelve los bytes de lo que se dibuje en su {@link Lienzo}. */
public final class PdfDibujado {

    private PdfDibujado() {
    }

    public static byte[] generar(Familia familia, BiConsumer<Lienzo, MedidorTexto> dibujo) {
        return generar(familia, MarcoPagina.PAGINA_ENTERA, dibujo);
    }

    public static byte[] generar(Familia familia, MarcoPagina marco, BiConsumer<Lienzo, MedidorTexto> dibujo) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try (PdfDocument pdf = new PdfDocument(new PdfWriter(salida))) {
            Fuentes fuentes = new Fuentes(familia);
            dibujo.accept(new Lienzo(pdf, fuentes, marco), fuentes);
        }
        return salida.toByteArray();
    }
}
