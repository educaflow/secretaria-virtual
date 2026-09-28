package com.educaflow.base.infrastructure.pdfgenerator.impl.formulario.maquetacion;

import com.educaflow.base.infrastructure.pdfgenerator.impl.formulario.visibilidad.CeldaVisible;
import com.educaflow.base.infrastructure.pdfgenerator.impl.formulario.visibilidad.FilaVisible;

import java.util.ArrayList;
import java.util.List;

/**
 * Reparte las celdas de una fila en líneas de 12 columnas.
 *
 * <p>Es tolerante a propósito: el build ya comprobó que la fila completa suma múltiplos de 12, pero
 * tras colapsar celdas la última línea puede quedar corta (se dibuja igual, cerrando la tabla) y
 * una celda que ya no cabe abre línea nueva.
 */
public final class ParticionFila {

    private ParticionFila() {
    }

    public static List<List<CeldaUbicada>> partir(FilaVisible fila) {
        List<List<CeldaUbicada>> lineas = new ArrayList<>();
        List<CeldaUbicada> linea = new ArrayList<>();
        int cursor = 0;
        for (CeldaVisible celda : fila.celdas()) {
            int unidades = celda.celda().unidades();
            if (cursor + unidades > MedidasTabla.FULL && !linea.isEmpty()) {
                lineas.add(List.copyOf(linea));
                linea = new ArrayList<>();
                cursor = 0;
            }
            linea.add(new CeldaUbicada(celda, cursor, cursor + unidades));
            cursor += unidades;
            if (cursor >= MedidasTabla.FULL) {
                lineas.add(List.copyOf(linea));
                linea = new ArrayList<>();
                cursor = 0;
            }
        }
        if (!linea.isEmpty()) {
            lineas.add(List.copyOf(linea));
        }
        return lineas;
    }
}
