package com.educaflow.base.infrastructure.pdfgenerator.impl.texto;

/** Lo que puede ser un trozo de un texto una vez troceado para ajustarlo a líneas. */
public enum TipoToken {
    PALABRA,
    ESPACIO,
    /** El valor de un {@code ${expresion;n}}: se dibuja como una palabra más, con un ancho mínimo. */
    VALOR
}
