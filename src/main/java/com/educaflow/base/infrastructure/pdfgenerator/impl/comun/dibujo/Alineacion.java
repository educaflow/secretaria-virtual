package com.educaflow.base.infrastructure.pdfgenerator.impl.comun.dibujo;

/** Cómo se coloca cada línea de un párrafo dentro de su ancho máximo. */
public enum Alineacion {
    IZQUIERDA,
    CENTRO,
    DERECHA,
    /** Las líneas llenan el ancho ensanchando sus espacios; la última de un párrafo no. */
    JUSTIFICADO;

    public static Alineacion parse(String valor, Alineacion porDefecto) {
        return switch (valor) {
            case "" -> porDefecto;
            case "izquierda" -> IZQUIERDA;
            case "centrado" -> CENTRO;
            case "derecha" -> DERECHA;
            case "justificado" -> JUSTIFICADO;
            default -> throw new RuntimeException("alineamiento=\"" + valor + "\" no es válido:"
                    + " solo admite izquierda, centrado, derecha o justificado");
        };
    }
}
