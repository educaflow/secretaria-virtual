package com.educaflow.base.infrastructure.pdfgenerator.impl.modelo;

/** Los tres elementos que puede llevar una {@code <fila>}. */
public enum TipoCelda {
    /** Etiqueta en mayúsculas y, debajo, el valor de {@code nombreCampo}. */
    CAMPO,
    /** Casilla marcada según el {@code Boolean} de {@code nombreCampo} y su etiqueta al lado. */
    CHECK,
    /** Solo texto. */
    TEXTO
}
