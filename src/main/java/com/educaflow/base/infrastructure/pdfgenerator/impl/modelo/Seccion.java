package com.educaflow.base.infrastructure.pdfgenerator.impl.modelo;

import java.util.List;

/** Una {@code <seccion>}: cabecera con letra automática y título bilingüe, y sus filas. */
public record Seccion(TextoBilingue titulo, List<Fila> filas, Visibilidad visibilidad) {
}
