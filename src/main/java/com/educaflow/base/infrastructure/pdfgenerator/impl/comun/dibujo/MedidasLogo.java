package com.educaflow.base.infrastructure.pdfgenerator.impl.comun.dibujo;

import static com.educaflow.base.infrastructure.pdfgenerator.impl.comun.dibujo.MedidasPagina.CM;

/**
 * Las medidas con las que se estampa el logo de la GVA, en puntos PDF. Están aquí, y no en las de
 * cada tipo de documento, porque el logo es uno solo y los dos lo dibujan con el mismo tamaño: el
 * formulario en la celda de su fila de cabecera y el documento en prosa en la cabecera de su
 * primera página.
 */
public final class MedidasLogo {

    private MedidasLogo() {
    }

    public static final double ANCHO = 4.343 * CM;
    public static final double ALTO = 1.939 * CM;
}
