package com.educaflow.base.infrastructure.pdfgenerator.impl.formulario.modelo;

import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.modelo.TextoBilingue;
import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.modelo.Visibilidad;

import java.util.List;

/** Una {@code <seccion>}: cabecera con letra automática y título bilingüe, y sus filas. */
public record Seccion(TextoBilingue titulo, List<Fila> filas, Visibilidad visibilidad) {
}
