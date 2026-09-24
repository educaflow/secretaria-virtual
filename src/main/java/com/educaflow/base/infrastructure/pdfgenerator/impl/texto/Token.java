package com.educaflow.base.infrastructure.pdfgenerator.impl.texto;

/**
 * Un trozo de texto con su fuente, su tamaño y el ancho que ocupa. Un {@link TipoToken#VALOR} vacío
 * ocupa el hueco de las {@code n} columnas pedidas en su {@code ${expresion;n}}.
 */
public record Token(TipoToken tipo, String texto, Fuente fuente, double tamanyo, double ancho) {
}
