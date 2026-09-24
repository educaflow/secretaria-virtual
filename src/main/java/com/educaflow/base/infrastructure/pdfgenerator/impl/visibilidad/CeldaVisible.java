package com.educaflow.base.infrastructure.pdfgenerator.impl.visibilidad;

import com.educaflow.base.infrastructure.pdfgenerator.impl.modelo.Celda;

/**
 * Una celda que va a ocupar sitio en el PDF.
 *
 * @param reservada {@code true} si está oculta con {@code siOculto="reservar"} (ella, su fila o su
 *                  sección): se dibujan solo sus bordes y sus expresiones de valor no se evalúan.
 */
public record CeldaVisible(Celda celda, boolean reservada) {
}
