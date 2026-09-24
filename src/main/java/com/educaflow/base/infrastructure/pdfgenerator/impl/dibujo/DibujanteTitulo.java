package com.educaflow.base.infrastructure.pdfgenerator.impl.dibujo;

import com.educaflow.base.infrastructure.pdfgenerator.impl.maquetacion.Medidas;
import com.educaflow.base.infrastructure.pdfgenerator.impl.modelo.TextoBilingue;
import com.educaflow.base.infrastructure.pdfgenerator.impl.texto.Fuente;
import com.educaflow.base.infrastructure.pdfgenerator.impl.texto.Maquetador;
import com.educaflow.base.infrastructure.pdfgenerator.impl.texto.Parrafo;

import static com.educaflow.base.infrastructure.pdfgenerator.impl.maquetacion.Medidas.FULL;
import static com.educaflow.base.infrastructure.pdfgenerator.impl.maquetacion.Medidas.LOGO_EDGE;
import static com.educaflow.base.infrastructure.pdfgenerator.impl.maquetacion.Medidas.LOGO_H;
import static com.educaflow.base.infrastructure.pdfgenerator.impl.maquetacion.Medidas.LOGO_W;
import static com.educaflow.base.infrastructure.pdfgenerator.impl.maquetacion.Medidas.PAD;
import static com.educaflow.base.infrastructure.pdfgenerator.impl.maquetacion.Medidas.ROW_TITULO;
import static com.educaflow.base.infrastructure.pdfgenerator.impl.maquetacion.Medidas.TABLE_W;

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
        double anchoTexto = Medidas.ancho(FULL - LOGO_EDGE) - 2 * PAD;
        Parrafo valenciano = maquetador.parrafo(titulo.valenciano(), Fuente.SEMINEGRITA, TAMANYO, true, FACTOR_ALTO, anchoTexto);
        Parrafo castellano = maquetador.parrafo(titulo.castellano(), Fuente.SEMINEGRITA_CURSIVA, TAMANYO, true, FACTOR_ALTO, anchoTexto);
        double altoTexto = valenciano.alto() + castellano.alto();
        // la fila crece para que el logo no pise los bordes
        double alto = Math.max(Math.max(ROW_TITULO, LOGO_H + 2 * PAD), altoTexto + 2 * PAD);

        lienzo.asegurarEspacio(alto);
        double top = lienzo.cursorY();
        double xLogo = Medidas.x(0);
        double anchoLogo = Medidas.ancho(LOGO_EDGE);
        lienzo.bordesCelda(xLogo, top, anchoLogo, alto, true, true);
        lienzo.bordesCelda(Medidas.x(LOGO_EDGE), top, TABLE_W - anchoLogo, alto, true, true);
        lienzo.logo(xLogo + (anchoLogo - LOGO_W) / 2, top - alto + (alto - LOGO_H) / 2, LOGO_W, LOGO_H);

        double y = top - (alto - altoTexto) / 2;
        lienzo.parrafo(valenciano, Medidas.x(LOGO_EDGE) + PAD, y, anchoTexto, Alineacion.CENTRO);
        y -= valenciano.alto();
        lienzo.parrafo(castellano, Medidas.x(LOGO_EDGE) + PAD, y, anchoTexto, Alineacion.CENTRO);
        lienzo.avanzar(alto);
    }
}
