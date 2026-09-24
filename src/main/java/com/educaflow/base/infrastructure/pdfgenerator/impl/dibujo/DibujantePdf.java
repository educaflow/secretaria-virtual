package com.educaflow.base.infrastructure.pdfgenerator.impl.dibujo;

import com.educaflow.base.infrastructure.pdfgenerator.impl.maquetacion.CeldaUbicada;
import com.educaflow.base.infrastructure.pdfgenerator.impl.maquetacion.ParticionFila;
import com.educaflow.base.infrastructure.pdfgenerator.impl.texto.Maquetador;
import com.educaflow.base.infrastructure.pdfgenerator.impl.visibilidad.DocumentoVisible;
import com.educaflow.base.infrastructure.pdfgenerator.impl.visibilidad.FilaVisible;
import com.educaflow.base.infrastructure.pdfgenerator.impl.visibilidad.SeccionVisible;
import com.educaflow.base.infrastructure.pdfgenerator.impl.visibilidad.Valores;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;

import java.io.ByteArrayOutputStream;
import java.util.List;

/**
 * Dibuja un {@link DocumentoVisible} con sus {@link Valores} en un PDF plano: título, y por cada
 * sección su cabecera con letra correlativa (A, B, C…) y las líneas de sus filas.
 */
public final class DibujantePdf {

    private DibujantePdf() {
    }

    public static byte[] dibujar(DocumentoVisible documento, Valores valores) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try (PdfDocument pdf = new PdfDocument(new PdfWriter(salida))) {
            Fuentes fuentes = new Fuentes();
            Lienzo lienzo = new Lienzo(pdf, fuentes);
            Maquetador maquetador = new Maquetador(fuentes, valores::texto);
            Maquetador maquetadorReservado = new Maquetador(fuentes, expresion -> "");
            DibujanteTitulo titulo = new DibujanteTitulo(lienzo, maquetador);
            DibujanteSeccion seccion = new DibujanteSeccion(lienzo, maquetador);
            DibujanteLinea linea = new DibujanteLinea(lienzo, maquetador, maquetadorReservado, valores);

            documento.titulo().ifPresent(titulo::dibujar);
            char letra = 'A';
            for (SeccionVisible s : documento.secciones()) {
                seccion.dibujarCabecera(s, letra);
                letra++;
                for (FilaVisible fila : s.filas()) {
                    List<List<CeldaUbicada>> lineas = ParticionFila.partir(fila);
                    for (int i = 0; i < lineas.size(); i++) {
                        linea.dibujar(lineas.get(i), i > 0, i < lineas.size() - 1);
                    }
                }
            }
        }
        return salida.toByteArray();
    }
}
