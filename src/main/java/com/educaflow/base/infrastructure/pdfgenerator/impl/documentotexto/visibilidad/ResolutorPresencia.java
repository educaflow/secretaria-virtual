package com.educaflow.base.infrastructure.pdfgenerator.impl.documentotexto.visibilidad;

import com.educaflow.base.infrastructure.evaluator.Evaluator;
import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.expresion.Presencia;
import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.modelo.Visibilidad;
import com.educaflow.base.infrastructure.pdfgenerator.impl.documentotexto.modelo.DocumentoTexto;
import com.educaflow.base.infrastructure.pdfgenerator.impl.documentotexto.modelo.Elemento;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Evalúa los {@code visible} del documento y decide qué se dibuja.
 *
 * <p>Se evalúa <b>por niveles</b> (los bloques del cuerpo, luego los hijos de los que se dibujan):
 * el {@code visible} de un hijo de algo colapsado o reservado no se evalúa nunca, así que no puede
 * reventar por unos datos que ese hijo ya no va a enseñar.
 *
 * <p>Lo colapsado desaparece del resultado; lo reservado se queda con su marca, propagada a todos
 * sus hijos.
 */
public final class ResolutorPresencia {

    private final Evaluator evaluator;
    private final Map<String, Object> contexto;

    public ResolutorPresencia(Evaluator evaluator, Map<String, Object> contexto) {
        this.evaluator = evaluator;
        this.contexto = contexto;
    }

    public DocumentoTextoVisible resolver(DocumentoTexto documento) {
        List<Visibilidad> nivel = new ArrayList<>();
        documento.cuerpo().forEach(bloque -> nivel.add(bloque.visibilidad()));

        return new DocumentoTextoVisible(documento.titulo(),
                elementos(documento.cuerpo(), evaluar(nivel)));
    }

    /** Los elementos de un mismo nivel, con sus {@code visible} ya evaluados en {@code resultados}. */
    private List<ElementoVisible> elementos(List<? extends Elemento> elementos, Map<String, Object> resultados) {
        List<ElementoVisible> visibles = new ArrayList<>();
        for (Elemento elemento : elementos) {
            switch (Presencia.de(elemento.visibilidad(), resultados)) {
                case VISIBLE -> visibles.add(new ElementoVisible(elemento, false, hijos(elemento, false)));
                case RESERVADA -> visibles.add(new ElementoVisible(elemento, true, hijos(elemento, true)));
                case COLAPSADA -> { }
            }
        }
        return List.copyOf(visibles);
    }

    private List<ElementoVisible> hijos(Elemento elemento, boolean reservado) {
        List<? extends Elemento> hijos = elemento.hijos();
        if (hijos.isEmpty()) {
            return List.of();
        }
        if (reservado) {
            return hijos.stream().map(hijo -> new ElementoVisible(hijo, true, hijos(hijo, true))).toList();
        }
        return elementos(hijos, evaluar(hijos.stream().map(Elemento::visibilidad).toList()));
    }

    /** Evalúa de una vez, en modo estricto, los {@code visible} de los elementos de un mismo nivel. */
    private Map<String, Object> evaluar(List<Visibilidad> visibilidades) {
        List<String> expresiones = visibilidades.stream()
                .map(Visibilidad::expresion)
                .flatMap(Optional::stream)
                .toList();
        if (expresiones.isEmpty()) {
            return Map.of();
        }
        return evaluator.evaluateStrict(expresiones, contexto);
    }
}
