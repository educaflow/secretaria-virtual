package com.educaflow.base.infrastructure.pdfgenerator.impl.documentotexto.dibujo;

import com.educaflow.base.infrastructure.pdfgenerator.HuecoCabeceraTexto;
import com.educaflow.base.util.Idioma;
import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.dibujo.Alineacion;
import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.dibujo.Lienzo;
import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.dibujo.MedidasLogo;
import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.modelo.TextoBilingue;
import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.texto.Fuente;
import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.texto.Maquetador;
import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.texto.Parrafo;

import static com.educaflow.base.infrastructure.pdfgenerator.impl.comun.dibujo.MedidasPagina.PAGE_H;
import static com.educaflow.base.infrastructure.pdfgenerator.impl.comun.dibujo.MedidasPagina.PAGE_W;
import static com.educaflow.base.infrastructure.pdfgenerator.impl.documentotexto.maquetacion.MedidasTexto.FACTOR_ALTO;
import static com.educaflow.base.infrastructure.pdfgenerator.impl.documentotexto.maquetacion.MedidasTexto.MARGEN_IZQUIERDO_CABECERA;
import static com.educaflow.base.infrastructure.pdfgenerator.impl.documentotexto.maquetacion.MedidasTexto.SEPARACION_CABECERA_CUERPO;
import static com.educaflow.base.infrastructure.pdfgenerator.impl.documentotexto.maquetacion.MedidasTexto.SEPARACION_LOGO_TITULO;
import static com.educaflow.base.infrastructure.pdfgenerator.impl.documentotexto.maquetacion.MedidasTexto.SEPARACION_TITULO_HUECO;
import static com.educaflow.base.infrastructure.pdfgenerator.impl.documentotexto.maquetacion.MedidasTexto.TAMANYO_TITULO;

/**
 * La cabecera del documento en prosa, en tres partes: el logo de la GVA a la izquierda, el título del
 * documento en mayúsculas centrado en la caja del medio y, a la derecha, un hueco en blanco. Es la
 * misma cabecera del formulario pero sin su recuadro y en un solo idioma, porque un documento en
 * prosa se emite en uno solo.
 *
 * <p>En el hueco no se dibuja nada: es el sitio del sello que se estampa después sobre el PDF (el
 * código QR del registro de salida y sus dos líneas de texto), que va a una distancia fija de los
 * bordes de la página. Por eso el hueco no cuelga del cursor sino de la página, y el logo y el
 * título se centran a su altura.
 *
 * <p>Se dibuja <b>dentro del flujo</b>, así que va solo en la primera página: no es una banda fija
 * del {@link com.educaflow.base.infrastructure.pdfgenerator.impl.comun.dibujo.MarcoPagina}.
 */
final class CabeceraDocumentoTexto {

    private CabeceraDocumentoTexto() {
    }

    static void dibujar(Lienzo lienzo, Maquetador maquetador, TextoBilingue titulo, Idioma idioma) {
        double x = MARGEN_IZQUIERDO_CABECERA + MedidasLogo.ANCHO + SEPARACION_LOGO_TITULO;
        double xHueco = PAGE_W - HuecoCabeceraTexto.MARGEN - HuecoCabeceraTexto.ANCHO;
        double ancho = xHueco - SEPARACION_TITULO_HUECO - x;
        Parrafo parrafo = maquetador.parrafo(titulo.en(idioma), Fuente.SEMINEGRITA, TAMANYO_TITULO,
                true, FACTOR_ALTO, ancho);
        // las tres partes se centran a la altura del hueco, así que manda la más alta de las tres
        double alto = Math.max(HuecoCabeceraTexto.ALTO, Math.max(MedidasLogo.ALTO, parrafo.alto()));
        double centro = PAGE_H - HuecoCabeceraTexto.MARGEN - HuecoCabeceraTexto.ALTO / 2;

        lienzo.logo(MARGEN_IZQUIERDO_CABECERA, centro - MedidasLogo.ALTO / 2, MedidasLogo.ANCHO, MedidasLogo.ALTO);
        lienzo.parrafo(parrafo, x, centro + parrafo.alto() / 2, ancho, Alineacion.CENTRO);
        lienzo.avanzar(lienzo.cursorY() - (centro - alto / 2) + SEPARACION_CABECERA_CUERPO);
    }
}
