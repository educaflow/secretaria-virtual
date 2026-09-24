package com.educaflow.base.infrastructure.pdfgenerator.impl.texto;

/** Un token ya colocado en su línea, con su desplazamiento desde el principio de ella. */
public record TokenColocado(Token token, double x) {
}
