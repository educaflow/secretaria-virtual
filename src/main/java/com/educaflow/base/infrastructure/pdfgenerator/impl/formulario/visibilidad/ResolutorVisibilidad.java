package com.educaflow.base.infrastructure.pdfgenerator.impl.formulario.visibilidad;

import com.educaflow.base.infrastructure.evaluator.Evaluator;
import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.expresion.Presencia;
import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.modelo.Visibilidad;
import com.educaflow.base.infrastructure.pdfgenerator.impl.formulario.modelo.Celda;
import com.educaflow.base.infrastructure.pdfgenerator.impl.formulario.modelo.Documento;
import com.educaflow.base.infrastructure.pdfgenerator.impl.formulario.modelo.Fila;
import com.educaflow.base.infrastructure.pdfgenerator.impl.formulario.modelo.Seccion;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Evalúa los {@code visible} del documento y decide qué se dibuja.
 *
 * <p>Se evalúa <b>por niveles</b> (secciones, luego las filas de las secciones que se dibujan, luego
 * las celdas de esas filas): el {@code visible} de un hijo de algo colapsado o reservado no se evalúa
 * nunca, así que no puede reventar por unos datos que ese hijo ya no va a enseñar.
 *
 * <p>Lo colapsado desaparece del resultado; lo reservado se queda con su marca, propagada a todas
 * sus celdas. Una fila que se queda sin celdas desaparece.
 */
public final class ResolutorVisibilidad {

    private final Evaluator evaluator;
    private final Map<String, Object> contexto;

    public ResolutorVisibilidad(Evaluator evaluator, Map<String, Object> contexto) {
        this.evaluator = evaluator;
        this.contexto = contexto;
    }

    public DocumentoVisible resolver(Documento documento) {
        List<SeccionVisible> secciones = new ArrayList<>();
        Map<String, Object> resultados = evaluar(documento.secciones(), Seccion::visibilidad);
        for (Seccion seccion : documento.secciones()) {
            switch (Presencia.de(seccion.visibilidad(), resultados)) {
                case VISIBLE -> secciones.add(new SeccionVisible(seccion.titulo(), filas(seccion, false), false));
                case RESERVADA -> secciones.add(new SeccionVisible(seccion.titulo(), filas(seccion, true), true));
                case COLAPSADA -> { }
            }
        }
        return new DocumentoVisible(documento.titulo(), List.copyOf(secciones));
    }

    private List<FilaVisible> filas(Seccion seccion, boolean reservadas) {
        List<FilaVisible> filas = new ArrayList<>();
        Map<String, Object> resultados = reservadas ? Map.of() : evaluar(seccion.filas(), Fila::visibilidad);
        for (Fila fila : seccion.filas()) {
            Presencia presencia = reservadas ? Presencia.RESERVADA : Presencia.de(fila.visibilidad(), resultados);
            if (presencia == Presencia.COLAPSADA) {
                continue;
            }
            List<CeldaVisible> celdas = celdas(fila, presencia == Presencia.RESERVADA);
            if (!celdas.isEmpty()) {
                filas.add(new FilaVisible(celdas));
            }
        }
        return List.copyOf(filas);
    }

    private List<CeldaVisible> celdas(Fila fila, boolean reservadas) {
        List<CeldaVisible> celdas = new ArrayList<>();
        Map<String, Object> resultados = reservadas ? Map.of() : evaluar(fila.celdas(), Celda::visibilidad);
        for (Celda celda : fila.celdas()) {
            Presencia presencia = reservadas ? Presencia.RESERVADA : Presencia.de(celda.visibilidad(), resultados);
            switch (presencia) {
                case VISIBLE -> celdas.add(new CeldaVisible(celda, false));
                case RESERVADA -> celdas.add(new CeldaVisible(celda, true));
                case COLAPSADA -> { }
            }
        }
        return List.copyOf(celdas);
    }

    /** Evalúa de una vez, en modo estricto, los {@code visible} de los elementos de un mismo nivel. */
    private <T> Map<String, Object> evaluar(List<T> elementos, Function<T, Visibilidad> visibilidad) {
        List<String> expresiones = new ArrayList<>();
        for (T elemento : elementos) {
            visibilidad.apply(elemento).expresion().ifPresent(expresiones::add);
        }
        if (expresiones.isEmpty()) {
            return Map.of();
        }
        return evaluator.evaluateStrict(expresiones, contexto);
    }
}
