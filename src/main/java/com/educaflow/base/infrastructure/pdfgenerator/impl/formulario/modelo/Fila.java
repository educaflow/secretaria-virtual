package com.educaflow.base.infrastructure.pdfgenerator.impl.formulario.modelo;

import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.modelo.Visibilidad;

import java.util.List;

/** Una {@code <fila>}: sus celdas ocupan una o varias líneas de la rejilla de 12 columnas. */
public record Fila(List<Celda> celdas, Visibilidad visibilidad) {
}
