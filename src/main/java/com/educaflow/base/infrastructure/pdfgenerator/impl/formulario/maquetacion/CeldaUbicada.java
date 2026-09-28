package com.educaflow.base.infrastructure.pdfgenerator.impl.formulario.maquetacion;

import com.educaflow.base.infrastructure.pdfgenerator.impl.formulario.modelo.Celda;
import com.educaflow.base.infrastructure.pdfgenerator.impl.formulario.visibilidad.CeldaVisible;

/** Una celda con su tramo de la rejilla dentro de su línea, en unidades (1200 = 12 columnas). */
public record CeldaUbicada(CeldaVisible visible, int inicio, int fin) {

    public Celda celda() {
        return visible.celda();
    }

    public boolean reservada() {
        return visible.reservada();
    }

    public int unidades() {
        return fin - inicio;
    }

    public double x() {
        return MedidasTabla.x(inicio);
    }

    public double ancho() {
        return MedidasTabla.ancho(unidades());
    }
}
