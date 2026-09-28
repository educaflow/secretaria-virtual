package com.educaflow.base.infrastructure.pdfgenerator.impl.documentotexto.modelo;

/** Cada uno de los elementos que el cuerpo del documento apila de arriba abajo. */
public sealed interface Bloque extends Elemento permits Texto, Lista, Espacio, Tabla {
}
