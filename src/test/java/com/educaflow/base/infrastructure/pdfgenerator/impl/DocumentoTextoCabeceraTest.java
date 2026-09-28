package com.educaflow.base.infrastructure.pdfgenerator.impl;

import com.educaflow.base.infrastructure.pdfgenerator.Idioma;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * La cabecera del documento en prosa: el logo de la GVA a la izquierda y el título centrado a su
 * derecha, solo en la primera página. Debajo arranca el cuerpo, que en todas las páginas baja hasta
 * el margen inferior porque ya no hay pie.
 *
 * <p>El documento son sesenta párrafos de una línea, así que la primera y la última línea de cada
 * página dicen exactamente dónde empieza y dónde acaba la banda del cuerpo.
 */
class DocumentoTextoCabeceraTest {

    private static final String DOCUMENTO = "documento_banda_del_cuerpo.xml";

    /** El tamaño del texto (12) por su factor de alto (1,23). */
    private static final double ALTO_LINEA = 14.76;
    /** La línea base va al 82 % del alto de línea por debajo del borde superior del renglón. */
    private static final double BASE_EN_EL_RENGLON = 0.82 * ALTO_LINEA;
    /** Lo que el renglón deja por debajo de su línea base. */
    private static final double DESCENSO_EN_EL_RENGLON = ALTO_LINEA - BASE_EN_EL_RENGLON;

    private static final double CM = 72 / 2.54;
    /** A4 (29,7 cm) menos el margen de 1 cm: donde arranca el cursor de cada página. */
    private static final double TOP_PAGINA = 29.7 * CM - CM;
    private static final double BOTTOM_PAGINA = CM;

    private static final double LOGO_ANCHO = 4.343 * CM;
    private static final double LOGO_ALTO = 1.939 * CM;
    private static final double MARGEN_IZQUIERDO_CABECERA = 48;
    private static final double BORDE_DERECHO_CABECERA = 539.5;
    private static final double SEPARACION_LOGO_TITULO = 7;
    /** Aire entre la cabecera y el primer texto del cuerpo. */
    private static final double SEPARACION_CABECERA_CUERPO = 40;

    /** El título de este documento cabe en una línea, así que la cabecera mide lo que el logo. */
    private static final double ALTO_CABECERA = LOGO_ALTO;
    private static final double TOP_CUERPO = TOP_PAGINA - ALTO_CABECERA - SEPARACION_CABECERA_CUERPO;

    private static final double X_TITULO = MARGEN_IZQUIERDO_CABECERA + LOGO_ANCHO + SEPARACION_LOGO_TITULO;
    private static final double CENTRO_TITULO = (X_TITULO + BORDE_DERECHO_CABECERA) / 2;

    @Test
    void elLogoVaArribaALaIzquierdaYSoloEnLaPrimeraPagina() {
        TextosDelPdf textos = generar(Idioma.CASTELLANO);

        assertTrue(paginas(textos) > 1, "el documento cabe en una página y no dice si la cabecera se repite");
        assertEquals(1, textos.imagenes().size(), "no hay exactamente un logo: " + textos.imagenes());

        TextosDelPdf.Imagen logo = textos.imagenes().get(0);
        assertEquals(1, logo.pagina(), "el logo no está en la primera página");
        assertEquals(MARGEN_IZQUIERDO_CABECERA, logo.x(), 0.01, "el logo no arranca en el margen de la cabecera");
        assertEquals(LOGO_ANCHO, logo.ancho(), 0.01, "el logo no mide lo que debe de ancho");
        assertEquals(LOGO_ALTO, logo.alto(), 0.01, "el logo no mide lo que debe de alto");
        assertEquals(TOP_PAGINA - LOGO_ALTO, logo.y(), 0.01, "el logo no cuelga del borde superior de la página");
    }

    @Test
    void elTituloVaEnMayusculasCentradoALaDerechaDelLogo() {
        TextosDelPdf.Linea titulo = primeraLineaDe(generar(Idioma.CASTELLANO));

        assertEquals("TITULO DEL DOCUMENTO", titulo.texto(), "el título no se estampó en mayúsculas");
        assertEquals(1, titulo.pagina(), "el título no está en la primera página");
        assertTrue(titulo.xInicio() > MARGEN_IZQUIERDO_CABECERA + LOGO_ANCHO,
                "el título pisa el logo: x=" + titulo.xInicio());
        assertEquals(CENTRO_TITULO, (titulo.xInicio() + titulo.xFin()) / 2, 0.5,
                "el título no está centrado en el hueco que queda a la derecha del logo");
        // el título mide una línea, así que su línea base queda a media altura del logo
        assertEquals(TOP_PAGINA - (ALTO_CABECERA - ALTO_LINEA) / 2 - BASE_EN_EL_RENGLON, titulo.y(), 0.01,
                "el título no está centrado verticalmente respecto al logo");
    }

    @Test
    void elCuerpoArrancaBajoLaCabeceraYBajaHastaElMargenInferior() {
        TextosDelPdf textos = generar(Idioma.CASTELLANO);
        List<TextosDelPdf.Linea> primeraPagina = lineasDe(textos, 1);

        // la primera línea de la página es el título, así que el cuerpo empieza en la segunda
        TextosDelPdf.Linea primera = primeraPagina.get(1);
        TextosDelPdf.Linea ultima = primeraPagina.get(primeraPagina.size() - 1);

        assertEquals(TOP_CUERPO - BASE_EN_EL_RENGLON, primera.y(), 0.01,
                "la primera línea «" + primera.texto() + "» no arranca justo debajo de la cabecera");
        assertTrue(ultima.y() < BOTTOM_PAGINA + DESCENSO_EN_EL_RENGLON + ALTO_LINEA,
                "en la página cabía otra línea detrás de «" + ultima.texto() + "»: el cuerpo no llega hasta el margen");
    }

    @Test
    void laSegundaPaginaEsCuerpoDeArribaAbajo() {
        TextosDelPdf textos = generar(Idioma.CASTELLANO);
        List<TextosDelPdf.Linea> segundaPagina = lineasDe(textos, 2);

        assertEquals(TOP_PAGINA - BASE_EN_EL_RENGLON, segundaPagina.get(0).y(), 0.01,
                "la segunda página no arranca en lo alto: sin cabecera no hay nada que dejarle sitio");
        textos.todos().stream().filter(texto -> texto.pagina() == 2).forEach(texto ->
                assertTrue(texto.y() >= BOTTOM_PAGINA && texto.y() <= TOP_PAGINA,
                        "el texto «" + texto.texto() + "» se sale de la página: y=" + texto.y()));
    }

    @Test
    void elTituloSeEstampaEnElIdiomaQueSePide() {
        assertEquals("TITULO DEL DOCUMENTO", primeraLineaDe(generar(Idioma.CASTELLANO)).texto());
        assertEquals("TITOL DEL DOCUMENT", primeraLineaDe(generar(Idioma.VALENCIANO)).texto());
    }

    // ------------------------------------------------------------------ apoyo

    private static TextosDelPdf.Linea primeraLineaDe(TextosDelPdf textos) {
        return lineasDe(textos, 1).get(0);
    }

    private static List<TextosDelPdf.Linea> lineasDe(TextosDelPdf textos, int pagina) {
        return textos.lineas().stream().filter(linea -> linea.pagina() == pagina).toList();
    }

    private static int paginas(TextosDelPdf textos) {
        return textos.todos().get(textos.todos().size() - 1).pagina();
    }

    private static TextosDelPdf generar(Idioma idioma) {
        return TextosDelPdf.de(DocumentosDeTexto.pdf(DOCUMENTO, Map.of(), idioma));
    }
}
