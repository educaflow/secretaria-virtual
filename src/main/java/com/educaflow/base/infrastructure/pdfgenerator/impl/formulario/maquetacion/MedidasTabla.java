package com.educaflow.base.infrastructure.pdfgenerator.impl.formulario.maquetacion;

import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.dibujo.MedidasLogo;
import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.texto.EstiloTexto;
import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.texto.Fuente;

import static com.educaflow.base.infrastructure.pdfgenerator.impl.comun.dibujo.MedidasPagina.CM;
import static com.educaflow.base.infrastructure.pdfgenerator.impl.comun.dibujo.MedidasPagina.MARGIN;

/**
 * Las medidas de la tabla del formulario, en puntos PDF: una única tabla sobre una rejilla lógica de
 * 12 columnas (1200 unidades). Son las mismas con las que se dibujaba en el build, para que el PDF
 * salga igual.
 */
public final class MedidasTabla {

    private MedidasTabla() {
    }

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
    /** El logo se mide en {@link MedidasLogo}, que es donde lo comparten los dos tipos de documento. */
    public static final double LOGO_W = MedidasLogo.ANCHO;
    public static final double LOGO_H = MedidasLogo.ALTO;

    /** Fuente y tamaño con los que se estampan los valores (campos e inline). */
    public static final EstiloTexto ESTILO_VALOR = new EstiloTexto(Fuente.REGULAR, 9);
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
