package com.educaflow.base.infrastructure.pdfgenerator.impl.modelo;

import java.util.List;

/** Una {@code <fila>}: sus celdas ocupan una o varias líneas de la rejilla de 12 columnas. */
public record Fila(List<Celda> celdas, Visibilidad visibilidad) {
}
