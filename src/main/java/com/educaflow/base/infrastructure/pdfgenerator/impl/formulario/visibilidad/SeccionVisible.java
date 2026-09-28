package com.educaflow.base.infrastructure.pdfgenerator.impl.formulario.visibilidad;

import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.modelo.TextoBilingue;

import java.util.List;

/**
 * Una sección que va a ocupar sitio en el PDF (y por tanto consume letra).
 *
 * @param reservada {@code true} si está oculta con {@code siOculto="reservar"}: su cabecera se dibuja
 *                  solo con los bordes (sin gris, letra ni título) y todas sus celdas son reservadas.
 */
public record SeccionVisible(TextoBilingue titulo, List<FilaVisible> filas, boolean reservada) {
}
