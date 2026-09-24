package com.educaflow.base.infrastructure.pdfgenerator.impl.maquetacion;

import com.educaflow.base.infrastructure.pdfgenerator.impl.texto.Fuente;

/**
 * Las medidas del documento, en puntos PDF: página A4 con 1 cm de margen y una única tabla sobre una
 * rejilla lógica de 12 columnas (1200 unidades). Son las mismas con las que se dibujaba en el build,
 * para que el PDF salga igual.
 */
public final class Medidas {

    private Medidas() {
    }

    public static final double CM = 72.0 / 2.54;
    public static final double PAGE_W = 21.0 * CM;
    public static final double PAGE_H = 29.7 * CM;
    public static final double MARGIN = 1.0 * CM;
    public static final double TABLE_W = 19.0 * CM;
    /** Unidades de la rejilla: 1200 = 12 columnas; un colspan de 1 son 100 unidades. */
    public static final int FULL = 1200;
    public static final int LETRA_EDGE = (int) Math.round(0.8 * FULL / 19.0);
    public static final int LOGO_EDGE = 290;

    /** Alto del hueco del valor de un {@code <campo>} (una línea). */
    public static final double CTL_H = 0.541 * CM;
    /** Lo que crece ese hueco por cada unidad de rowSpan de más. */
    public static final double EXTRA_H = 0.62 * CM;
    public static final double LABEL_VACIO = 0.1 * CM;
    public static final double ROW_CHECK = 0.818 * CM;
    public static final double ROW_TITULO = 1.901 * CM;
    public static final double ROW_SECCION = 0.649 * CM;
    public static final double PAD = 0.101 * CM;
    /** Aire entre la etiqueta de un campo y el borde superior. */
    public static final double LABEL_PAD_TOP = 0.09 * CM;
    public static final double CHECK_SIDE = 0.35 * CM;
    /** Espacio reservado a la casilla antes de la etiqueta. */
    public static final double CHECK_COL_W = 0.9 * CM;
    /** Separación entre la casilla y sus etiquetas. */
    public static final double CHECK_LABEL_GAP = 0.25 * CM;
    public static final double LOGO_W = 4.343 * CM;
    public static final double LOGO_H = 1.939 * CM;

    /** Fuente y tamaño con los que se estampan los valores (campos e inline). */
    public static final Fuente FUENTE_VALOR = Fuente.REGULAR;
    public static final double TAMANYO_VALOR = 9;
    /** Aire por encima y por debajo del valor dentro del hueco de un {@code <campo>}. */
    public static final double PAD_VALOR = 2;

    /** La x de una posición de la rejilla. */
    public static double x(double unidades) {
        return MARGIN + unidades * TABLE_W / FULL;
    }

    /** El ancho de un tramo de la rejilla. */
    public static double ancho(double unidades) {
        return unidades * TABLE_W / FULL;
    }
}
