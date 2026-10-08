package com.educaflow.base.infrastructure.pdfgenerator.impl;

import com.educaflow.base.infrastructure.pdfgenerator.HuecoCabeceraTexto;
import com.educaflow.base.util.Idioma;
import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.dibujo.Lienzo;
import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.dibujo.MedidasLogo;
import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.dibujo.MedidasPagina;
import com.educaflow.base.infrastructure.pdfgenerator.impl.documentotexto.maquetacion.MedidasTexto;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * La cabecera del documento en prosa: el logo de la GVA a la izquierda, el título centrado en medio
 * y, a la derecha, el hueco en blanco del sello que se estampa después (el código QR del registro de
 * salida), solo en la primera página. Debajo arranca el cuerpo, que en todas las páginas baja hasta
 * el margen inferior porque ya no hay pie.
 *
 * <p>El documento son sesenta párrafos de una línea, así que la primera y la última línea de cada
 * página dicen exactamente dónde empieza y dónde acaba la banda del cuerpo.
 */
class DocumentoTextoCabeceraTest {

    private static final String DOCUMENTO = "documento_banda_del_cuerpo.xml";

    // Las medidas se toman del propio generador: el test comprueba cómo se colocan las partes de la
    // cabecera unas respecto a otras, no cuánto vale cada medida, así que cambiar una no lo rompe.

    private static final double ALTO_LINEA = MedidasTexto.TAMANYO * MedidasTexto.FACTOR_ALTO;
    private static final double BASE_EN_EL_RENGLON = Lienzo.PROPORCION_LINEA_BASE * ALTO_LINEA;
    /** Lo que el renglón deja por debajo de su línea base. */
    private static final double DESCENSO_EN_EL_RENGLON = ALTO_LINEA - BASE_EN_EL_RENGLON;

    /** Donde arranca el cursor de cada página: el alto de la página menos su margen. */
    private static final double TOP_PAGINA = MedidasPagina.PAGE_H - MedidasPagina.MARGIN;
    private static final double BOTTOM_PAGINA = MedidasPagina.MARGIN;

    private static final double LOGO_ANCHO = MedidasLogo.ANCHO;
    private static final double LOGO_ALTO = MedidasLogo.ALTO;
    private static final double MARGEN_IZQUIERDO_CABECERA = MedidasTexto.MARGEN_IZQUIERDO_CABECERA;

    /**
     * El hueco de la derecha: el recuadro que el registro de salida estampa separado de los bordes
     * de arriba y de la derecha de la página.
     */
    private static final double X_HUECO = MedidasPagina.PAGE_W - HuecoCabeceraTexto.MARGEN - HuecoCabeceraTexto.ANCHO;
    private static final double TOP_HUECO = MedidasPagina.PAGE_H - HuecoCabeceraTexto.MARGEN;
    private static final double BOTTOM_HUECO = TOP_HUECO - HuecoCabeceraTexto.ALTO;

    /** El hueco es la parte más alta de la cabecera, así que el logo y el título se centran a su altura. */
    private static final double CENTRO_CABECERA = TOP_HUECO - HuecoCabeceraTexto.ALTO / 2;
    private static final double TOP_CUERPO = BOTTOM_HUECO - MedidasTexto.SEPARACION_CABECERA_CUERPO;

    private static final double X_TITULO = MARGEN_IZQUIERDO_CABECERA + LOGO_ANCHO + MedidasTexto.SEPARACION_LOGO_TITULO;
    private static final double BORDE_DERECHO_TITULO = X_HUECO - MedidasTexto.SEPARACION_TITULO_HUECO;
    private static final double CENTRO_TITULO = (X_TITULO + BORDE_DERECHO_TITULO) / 2;

    private static final String TITULO_LARGO = "Resolucion de la solicitud de justificacion de falta de asistencia"
            + " del profesorado del centro";

    @Test
    void elLogoVaALaIzquierdaALaAlturaDelHuecoYSoloEnLaPrimeraPagina() {
        TextosDelPdf textos = generar(Idioma.CASTELLANO);

        assertTrue(paginas(textos) > 1, "el documento cabe en una página y no dice si la cabecera se repite");
        assertEquals(1, textos.imagenes().size(), "no hay exactamente un logo: " + textos.imagenes());

        TextosDelPdf.Imagen logo = textos.imagenes().get(0);
        assertEquals(1, logo.pagina(), "el logo no está en la primera página");
        assertEquals(MARGEN_IZQUIERDO_CABECERA, logo.x(), 0.01, "el logo no arranca en el margen de la cabecera");
        assertEquals(LOGO_ANCHO, logo.ancho(), 0.01, "el logo no mide lo que debe de ancho");
        assertEquals(LOGO_ALTO, logo.alto(), 0.01, "el logo no mide lo que debe de alto");
        assertEquals(CENTRO_CABECERA - LOGO_ALTO / 2, logo.y(), 0.01,
                "el logo no está centrado verticalmente respecto al hueco");
    }

    @Test
    void elTituloVaEnMayusculasCentradoEntreElLogoYElHueco() {
        TextosDelPdf.Linea titulo = primeraLineaDe(generar(Idioma.CASTELLANO));

        assertEquals("TITULO DEL DOCUMENTO", titulo.texto(), "el título no se estampó en mayúsculas");
        assertEquals(1, titulo.pagina(), "el título no está en la primera página");
        assertTrue(titulo.xInicio() > MARGEN_IZQUIERDO_CABECERA + LOGO_ANCHO,
                "el título pisa el logo: x=" + titulo.xInicio());
        assertEquals(CENTRO_TITULO, (titulo.xInicio() + titulo.xFin()) / 2, 0.5,
                "el título no está centrado en la caja que queda entre el logo y el hueco");
        assertEquals(CENTRO_CABECERA + ALTO_LINEA / 2 - BASE_EN_EL_RENGLON, titulo.y(), 0.01,
                "el título no está centrado verticalmente respecto al hueco");
    }

    @Test
    void unTituloLargoSeParteEnLineasSinEntrarEnElHueco() {
        String xml = DocumentosDeTexto.fuente(DOCUMENTO).replace("Titulo del documento", TITULO_LARGO);
        TextosDelPdf textos = TextosDelPdf.de(DocumentosDeTexto.pdfDe(xml, Map.of(), Idioma.CASTELLANO));
        // por encima del borde inferior del hueco solo está el título: el cuerpo arranca más abajo
        List<TextosDelPdf.Linea> titulo = lineasDe(textos, 1).stream().filter(linea -> linea.y() > BOTTOM_HUECO).toList();

        assertTrue(titulo.size() > 1, "el título cabe en una línea y no dice si se parte antes del hueco: " + titulo);
        titulo.forEach(linea -> {
            assertTrue(linea.xInicio() >= X_TITULO - 0.01, "la línea «" + linea.texto() + "» pisa el logo");
            assertTrue(linea.xFin() <= BORDE_DERECHO_TITULO + 0.01, "la línea «" + linea.texto() + "» entra en el hueco");
        });
        assertEquals(CENTRO_CABECERA + titulo.size() * ALTO_LINEA / 2 - BASE_EN_EL_RENGLON, titulo.get(0).y(), 0.01,
                "el título de varias líneas no está centrado verticalmente respecto al hueco");
    }

    @Test
    void elHuecoDeLaDerechaQuedaEnBlanco() {
        TextosDelPdf textos = generar(Idioma.CASTELLANO);

        textos.todos().stream().filter(texto -> texto.pagina() == 1).forEach(texto ->
                assertTrue(texto.xFin() <= X_HUECO || texto.y() + BASE_EN_EL_RENGLON <= BOTTOM_HUECO,
                        "el texto «" + texto.texto() + "» cae en el hueco: x=" + texto.xFin() + " y=" + texto.y()));
        textos.imagenes().forEach(imagen ->
                assertTrue(imagen.x() + imagen.ancho() <= X_HUECO, "el logo cae en el hueco: " + imagen));
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
        assertTrue(primera.y() + BASE_EN_EL_RENGLON < BOTTOM_HUECO,
                "la primera línea «" + primera.texto() + "» no queda por debajo del hueco: lo que se estampe en él la taparía");
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
