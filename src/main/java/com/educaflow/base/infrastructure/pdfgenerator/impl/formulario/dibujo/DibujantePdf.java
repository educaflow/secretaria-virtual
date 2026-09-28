package com.educaflow.base.infrastructure.pdfgenerator.impl.formulario.dibujo;

import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.dibujo.PdfDibujado;
import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.expresion.Valores;
import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.texto.Familia;
import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.texto.Maquetador;
import com.educaflow.base.infrastructure.pdfgenerator.impl.formulario.maquetacion.CeldaUbicada;
import com.educaflow.base.infrastructure.pdfgenerator.impl.formulario.maquetacion.MedidasTabla;
import com.educaflow.base.infrastructure.pdfgenerator.impl.formulario.maquetacion.ParticionFila;
import com.educaflow.base.infrastructure.pdfgenerator.impl.formulario.visibilidad.DocumentoVisible;
import com.educaflow.base.infrastructure.pdfgenerator.impl.formulario.visibilidad.FilaVisible;
import com.educaflow.base.infrastructure.pdfgenerator.impl.formulario.visibilidad.SeccionVisible;

import java.util.List;

/**
 * Dibuja un {@link DocumentoVisible} con sus {@link Valores} en un PDF plano: título, y por cada
 * sección su cabecera con letra correlativa (A, B, C…) y las líneas de sus filas.
 */
public final class DibujantePdf {

    private DibujantePdf() {
    }

    public static byte[] dibujar(DocumentoVisible documento, Valores valores) {
        return PdfDibujado.generar(Familia.ROBOTO, (lienzo, fuentes) -> {
            Maquetador maquetador = new Maquetador(fuentes, valores::texto, MedidasTabla.ESTILO_VALOR);
            Maquetador maquetadorReservado = new Maquetador(fuentes, expresion -> "", MedidasTabla.ESTILO_VALOR);
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
        });
    }
}
