package com.educaflow.base.infrastructure.pdfgenerator.impl.visibilidad;

import java.util.List;

/** Una fila que va a ocupar sitio en el PDF, ya sin sus celdas colapsadas. Nunca está vacía. */
public record FilaVisible(List<CeldaVisible> celdas) {
}
