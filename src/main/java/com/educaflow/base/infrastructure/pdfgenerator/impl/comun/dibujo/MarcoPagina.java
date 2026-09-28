package com.educaflow.base.infrastructure.pdfgenerator.impl.comun.dibujo;

import java.util.function.Consumer;

/**
 * Lo que se dibuja fijo en cada página (una cabecera, un pie) y la banda vertical que le queda al
 * cuerpo entre ambos: {@code topCuerpo} es donde arranca el cursor de cada página nueva y
 * {@code bottomCuerpo} el límite por debajo del cual ya no cabe nada.
 */
public record MarcoPagina(double topCuerpo, double bottomCuerpo, Consumer<Lienzo> dibujo) {

    /** Sin nada fijo: el cuerpo ocupa la página entera menos sus márgenes. */
    public static final MarcoPagina PAGINA_ENTERA = new MarcoPagina(
            MedidasPagina.PAGE_H - MedidasPagina.MARGIN, MedidasPagina.MARGIN, lienzo -> { });
}
