package com.educaflow.base.infrastructure.pdfgenerator.impl.formulario.dibujo;

import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.dibujo.Alineacion;
import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.dibujo.Lienzo;
import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.modelo.TextoBilingue;
import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.texto.Fuente;
import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.texto.Maquetador;
import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.texto.Parrafo;
import com.educaflow.base.infrastructure.pdfgenerator.impl.formulario.maquetacion.MedidasTabla;

import static com.educaflow.base.infrastructure.pdfgenerator.impl.formulario.maquetacion.MedidasTabla.FULL;
import static com.educaflow.base.infrastructure.pdfgenerator.impl.formulario.maquetacion.MedidasTabla.LOGO_EDGE;
import static com.educaflow.base.infrastructure.pdfgenerator.impl.formulario.maquetacion.MedidasTabla.LOGO_H;
import static com.educaflow.base.infrastructure.pdfgenerator.impl.formulario.maquetacion.MedidasTabla.LOGO_W;
import static com.educaflow.base.infrastructure.pdfgenerator.impl.formulario.maquetacion.MedidasTabla.PAD;
import static com.educaflow.base.infrastructure.pdfgenerator.impl.formulario.maquetacion.MedidasTabla.ROW_TITULO;
import static com.educaflow.base.infrastructure.pdfgenerator.impl.formulario.maquetacion.MedidasTabla.TABLE_W;

/** La fila de cabecera: el logo en su celda y el título bilingüe centrado, en mayúsculas. */
final class DibujanteTitulo {

    private static final double TAMANYO = 11;
    private static final double FACTOR_ALTO = 1.15;

    private final Lienzo lienzo;
    private final Maquetador maquetador;

    DibujanteTitulo(Lienzo lienzo, Maquetador maquetador) {
        this.lienzo = lienzo;
        this.maquetador = maquetador;
    }

    void dibujar(TextoBilingue titulo) {
        double anchoTexto = MedidasTabla.ancho(FULL - LOGO_EDGE) - 2 * PAD;
        Parrafo valenciano = maquetador.parrafo(titulo.valenciano(), Fuente.SEMINEGRITA, TAMANYO, true, FACTOR_ALTO, anchoTexto);
        Parrafo castellano = maquetador.parrafo(titulo.castellano(), Fuente.SEMINEGRITA_CURSIVA, TAMANYO, true, FACTOR_ALTO, anchoTexto);
        double altoTexto = valenciano.alto() + castellano.alto();
        // la fila crece para que el logo no pise los bordes
        double alto = Math.max(Math.max(ROW_TITULO, LOGO_H + 2 * PAD), altoTexto + 2 * PAD);

        lienzo.asegurarEspacio(alto);
        double top = lienzo.cursorY();
        double xLogo = MedidasTabla.x(0);
        double anchoLogo = MedidasTabla.ancho(LOGO_EDGE);
        lienzo.bordesCelda(xLogo, top, anchoLogo, alto, true, true);
        lienzo.bordesCelda(MedidasTabla.x(LOGO_EDGE), top, TABLE_W - anchoLogo, alto, true, true);
        lienzo.logo(xLogo + (anchoLogo - LOGO_W) / 2, top - alto + (alto - LOGO_H) / 2, LOGO_W, LOGO_H);

        double y = top - (alto - altoTexto) / 2;
        lienzo.parrafo(valenciano, MedidasTabla.x(LOGO_EDGE) + PAD, y, anchoTexto, Alineacion.CENTRO);
        y -= valenciano.alto();
        lienzo.parrafo(castellano, MedidasTabla.x(LOGO_EDGE) + PAD, y, anchoTexto, Alineacion.CENTRO);
        lienzo.avanzar(alto);
    }
}
