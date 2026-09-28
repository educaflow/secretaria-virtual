package com.educaflow.base.infrastructure.pdfgenerator.impl.documentotexto.dibujo;

import com.educaflow.base.infrastructure.pdfgenerator.Idioma;
import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.dibujo.Alineacion;
import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.dibujo.Lienzo;
import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.dibujo.MedidasLogo;
import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.modelo.TextoBilingue;
import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.texto.Fuente;
import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.texto.Maquetador;
import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.texto.Parrafo;

import static com.educaflow.base.infrastructure.pdfgenerator.impl.documentotexto.maquetacion.MedidasTexto.BORDE_DERECHO_CABECERA;
import static com.educaflow.base.infrastructure.pdfgenerator.impl.documentotexto.maquetacion.MedidasTexto.FACTOR_ALTO;
import static com.educaflow.base.infrastructure.pdfgenerator.impl.documentotexto.maquetacion.MedidasTexto.MARGEN_IZQUIERDO_CABECERA;
import static com.educaflow.base.infrastructure.pdfgenerator.impl.documentotexto.maquetacion.MedidasTexto.SEPARACION_CABECERA_CUERPO;
import static com.educaflow.base.infrastructure.pdfgenerator.impl.documentotexto.maquetacion.MedidasTexto.SEPARACION_LOGO_TITULO;
import static com.educaflow.base.infrastructure.pdfgenerator.impl.documentotexto.maquetacion.MedidasTexto.TAMANYO_TITULO;

/**
 * La cabecera del documento en prosa: el logo de la GVA a la izquierda y, centrado en el hueco que
 * queda a su derecha, el título del documento en mayúsculas. Es la misma cabecera del formulario
 * pero sin su recuadro y en un solo idioma, porque un documento en prosa se emite en uno solo.
 *
 * <p>Se dibuja <b>dentro del flujo</b>, así que va solo en la primera página: no es una banda fija
 * del {@link com.educaflow.base.infrastructure.pdfgenerator.impl.comun.dibujo.MarcoPagina}.
 */
final class CabeceraDocumentoTexto {

    private CabeceraDocumentoTexto() {
    }

    static void dibujar(Lienzo lienzo, Maquetador maquetador, TextoBilingue titulo, Idioma idioma) {
        double x = MARGEN_IZQUIERDO_CABECERA + MedidasLogo.ANCHO + SEPARACION_LOGO_TITULO;
        double ancho = BORDE_DERECHO_CABECERA - x;
        Parrafo parrafo = maquetador.parrafo(titulo.en(idioma), Fuente.SEMINEGRITA, TAMANYO_TITULO,
                true, FACTOR_ALTO, ancho);
        // el logo y el título se centran uno respecto al otro, así que manda el más alto de los dos
        double alto = Math.max(MedidasLogo.ALTO, parrafo.alto());

        double top = lienzo.cursorY();
        lienzo.logo(MARGEN_IZQUIERDO_CABECERA, top - alto + (alto - MedidasLogo.ALTO) / 2,
                MedidasLogo.ANCHO, MedidasLogo.ALTO);
        lienzo.parrafo(parrafo, x, top - (alto - parrafo.alto()) / 2, ancho, Alineacion.CENTRO);
        lienzo.avanzar(alto + SEPARACION_CABECERA_CUERPO);
    }
}
