package com.educaflow.base.infrastructure.pdfgenerator.impl.modelo;

/** Qué se hace con un elemento cuyo {@code visible} evalúa a {@code false}. */
public enum SiOculto {
    /** Desaparece: lo que va detrás se desplaza y una sección no consume letra. Es el valor por omisión. */
    COLAPSAR,
    /** Conserva su sitio y su alto: se dibujan solo los bordes; una sección sí consume letra. */
    RESERVAR;

    public static SiOculto parse(String valor) {
        return switch (valor) {
            case "", "colapsar" -> COLAPSAR;
            case "reservar" -> RESERVAR;
            default -> throw new RuntimeException("siOculto=\"" + valor + "\" no es válido: solo admite colapsar o reservar");
        };
    }
}
