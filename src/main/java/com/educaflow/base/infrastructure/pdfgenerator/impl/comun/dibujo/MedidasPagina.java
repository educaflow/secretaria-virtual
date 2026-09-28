package com.educaflow.base.infrastructure.pdfgenerator.impl.comun.dibujo;

/**
 * Las medidas de la página en la que se dibuja, en puntos PDF: A4 con 1 cm de margen. Son las mismas
 * con las que se dibujaba en el build, para que el PDF salga igual.
 */
public final class MedidasPagina {

    private MedidasPagina() {
    }

    public static final double CM = 72.0 / 2.54;
    public static final double PAGE_W = 21.0 * CM;
    public static final double PAGE_H = 29.7 * CM;
    public static final double MARGIN = 1.0 * CM;
}
