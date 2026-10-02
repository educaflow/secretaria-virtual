package com.educaflow.base.infrastructure.pdfgenerator.impl.documentotexto.maquetacion;

import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.texto.EstiloTexto;
import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.texto.Fuente;

/**
 * Las medidas del documento en prosa, en puntos PDF: la cabecera de su primera página y, debajo, el
 * cuerpo de texto corrido que baja hasta el margen inferior de cada página. El alto de la cabecera
 * no está aquí porque es el de la más alta de sus tres partes, y el título puede ocupar varias líneas.
 */
public final class MedidasTexto {

    private MedidasTexto() {
    }

    public static final double MARGEN_IZQUIERDO = 78;
    public static final double BORDE_DERECHO = 525;
    public static final double ANCHO_CUERPO = BORDE_DERECHO - MARGEN_IZQUIERDO;

    public static final double TAMANYO = 12;
    public static final double FACTOR_ALTO = 1.23;

    public static final String VINYETA = "•";
    /**
     * La viñeta se estampa mucho mayor que el texto porque la tinta del bolo de Montserrat es solo el 15 %
     * de su tamaño, y así mide los 4,32 pt del bolo del documento que se reproduce. El avance del glifo se
     * queda grande de más, pero no se ve: la viñeta se dibuja sola en su hueco y el texto del item arranca
     * siempre en {@link #SANGRADO_ITEM}.
     */
    public static final double TAMANYO_VINYETA = 4.32 / 0.150;
    /**
     * Lo que baja la línea base de la viñeta respecto a la del texto del item. El bolo de Montserrat se apoya
     * alto —su centro queda al 27,3 % del tamaño sobre la línea base—, así que sin esta bajada se lee como
     * un punto alto en vez de centrado en la altura de la x, que es el 52,5 % de {@link #TAMANYO}.
     */
    public static final double DESCENSO_VINYETA = 0.273 * TAMANYO_VINYETA - 0.525 * TAMANYO / 2;
    /** Sangrado de la viñeta de un {@code <item>} respecto al margen izquierdo. */
    public static final double SANGRADO_VINYETA = 18;
    /** Sangrado del texto de un {@code <item>}: las líneas siguientes vuelven aquí, no a la viñeta. */
    public static final double SANGRADO_ITEM = 36;

    /**
     * La cabecera no se maqueta con el margen del cuerpo: tiene el suyo y sobresale del texto por
     * los dos lados. Por la derecha acaba en su hueco, que llega más allá del borde del cuerpo y cuyas
     * medidas están en {@link com.educaflow.base.infrastructure.pdfgenerator.HuecoCabeceraTexto}.
     */
    public static final double MARGEN_IZQUIERDO_CABECERA = 48;

    /** Aire entre el logo y la caja del título, a su derecha. */
    public static final double SEPARACION_LOGO_TITULO = 10;
    /** Aire entre la caja del título y el hueco, a su derecha. */
    public static final double SEPARACION_TITULO_HUECO = 7;
    /** Aire entre la cabecera y el primer texto del cuerpo. */
    public static final double SEPARACION_CABECERA_CUERPO = 40;

    /** El título de la cabecera se estampa al tamaño del cuerpo, en seminegrita y en mayúsculas. */
    public static final double TAMANYO_TITULO = TAMANYO;

    /** Fuente y tamaño con los que se estampa el valor de un {@code ${expresion}}. */
    public static final EstiloTexto ESTILO_VALOR = new EstiloTexto(Fuente.REGULAR, TAMANYO);
}
