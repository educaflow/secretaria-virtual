package com.educaflow.base.infrastructure.pdfgenerator;

/**
 * El hueco que la cabecera de un {@code <documentoTexto>} deja en blanco a su derecha, en puntos PDF,
 * para el sello que se estampa después sobre el PDF ya generado: el código QR del registro de salida
 * y, debajo, sus dos líneas de texto. Quien sella toma de aquí las medidas de su recuadro, y lo
 * coloca a {@link #MARGEN} de los bordes de arriba y de la derecha de la primera página.
 */
public final class HuecoCabeceraTexto {

    private HuecoCabeceraTexto() {
    }

    public static final double ANCHO = 88;
    public static final double ALTO = 104;
    /** Lo que se separa el hueco de los bordes de arriba y de la derecha de la página. */
    public static final double MARGEN = 20;
}
