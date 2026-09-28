package com.educaflow.base.infrastructure.pdfgenerator.impl.comun.texto;

/** Lo que puede ser un trozo de un texto una vez troceado para ajustarlo a líneas. */
public enum TipoToken {
    PALABRA,
    ESPACIO,
    /** Un {@code \n} del texto: cierra la línea y no se estampa. */
    SALTO
}
