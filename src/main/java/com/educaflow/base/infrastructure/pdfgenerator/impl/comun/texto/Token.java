package com.educaflow.base.infrastructure.pdfgenerator.impl.comun.texto;

/** Un trozo de texto con su fuente, su tamaño y el ancho que ocupa. */
public record Token(TipoToken tipo, String texto, Fuente fuente, double tamanyo, double ancho) {
}
