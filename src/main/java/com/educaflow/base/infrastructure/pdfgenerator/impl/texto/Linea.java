package com.educaflow.base.infrastructure.pdfgenerator.impl.texto;

import java.util.List;

/** Una línea de un párrafo ya ajustado: sus tokens colocados y el ancho total que ocupan. */
public record Linea(List<TokenColocado> tokens, double ancho) {
}
