package com.educaflow.base.infrastructure.pdfgenerator.impl;

import com.educaflow.base.infrastructure.pdfgenerator.impl.maquetacion.CeldaUbicada;
import com.educaflow.base.infrastructure.pdfgenerator.impl.maquetacion.ParticionFila;
import com.educaflow.base.infrastructure.pdfgenerator.impl.modelo.Celda;
import com.educaflow.base.infrastructure.pdfgenerator.impl.modelo.TextoBilingue;
import com.educaflow.base.infrastructure.pdfgenerator.impl.modelo.TipoCelda;
import com.educaflow.base.infrastructure.pdfgenerator.impl.modelo.Visibilidad;
import com.educaflow.base.infrastructure.pdfgenerator.impl.visibilidad.CeldaVisible;
import com.educaflow.base.infrastructure.pdfgenerator.impl.visibilidad.FilaVisible;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ParticionFilaTest {

    @Test
    void unaFilaCompletaSeParteEnLineasDe12Columnas() {
        List<List<CeldaUbicada>> lineas = ParticionFila.partir(fila(4, 4, 4, 12));

        assertEquals(2, lineas.size());
        assertEquals(List.of(0, 400, 800), lineas.get(0).stream().map(CeldaUbicada::inicio).toList());
        assertEquals(1200, lineas.get(0).get(2).fin());
        assertEquals(0, lineas.get(1).get(0).inicio());
    }

    @Test
    void unaUltimaLineaIncompletaSeTolera() {
        List<List<CeldaUbicada>> lineas = ParticionFila.partir(fila(6, 6, 6));

        assertEquals(2, lineas.size());
        assertEquals(1, lineas.get(1).size());
        assertEquals(600, lineas.get(1).get(0).fin());
    }

    @Test
    void unaCeldaQueYaNoCabeAbreLineaNueva() {
        List<List<CeldaUbicada>> lineas = ParticionFila.partir(fila(8, 8));

        assertEquals(2, lineas.size());
        assertEquals(0, lineas.get(1).get(0).inicio());
    }

    @Test
    void losDecimalesSeRedondeanAUnidades() {
        List<List<CeldaUbicada>> lineas = ParticionFila.partir(fila(6, 2.1, 3.9));

        assertEquals(1, lineas.size());
        assertEquals(List.of(0, 600, 810), lineas.get(0).stream().map(CeldaUbicada::inicio).toList());
        assertEquals(1200, lineas.get(0).get(2).fin());
    }

    private static FilaVisible fila(double... colspans) {
        List<CeldaVisible> celdas = java.util.Arrays.stream(colspans)
                .mapToObj(colspan -> new CeldaVisible(
                        new Celda(TipoCelda.TEXTO, Optional.empty(), colspan, 1, TextoBilingue.VACIO, Visibilidad.SIEMPRE), false))
                .toList();
        return new FilaVisible(celdas);
    }
}
