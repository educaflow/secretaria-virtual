package com.educaflow.base.infrastructure.pdfgenerator.impl.comun.texto;

/** Mide el ancho que ocupa un texto con una fuente y un tamaño, en puntos PDF. */
public interface MedidorTexto {

    double ancho(Fuente fuente, String texto, double tamanyo);
}
