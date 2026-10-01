package com.educaflow.base.infrastructure.pdfgenerator.impl.formulario.parser;

import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.modelo.TextoBilingue;
import com.educaflow.base.infrastructure.pdfgenerator.impl.formulario.modelo.Celda;
import com.educaflow.base.infrastructure.pdfgenerator.impl.formulario.modelo.Documento;
import com.educaflow.base.infrastructure.pdfgenerator.impl.formulario.modelo.Fila;
import com.educaflow.base.infrastructure.pdfgenerator.impl.formulario.modelo.Seccion;
import com.educaflow.base.infrastructure.pdfgenerator.impl.formulario.modelo.TipoCelda;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.educaflow.base.infrastructure.pdfgenerator.impl.comun.parser.ElementosXml.campoFirma;
import static com.educaflow.base.infrastructure.pdfgenerator.impl.comun.parser.ElementosXml.hijos;
import static com.educaflow.base.infrastructure.pdfgenerator.impl.comun.parser.ElementosXml.textos;
import static com.educaflow.base.infrastructure.pdfgenerator.impl.comun.parser.ElementosXml.visibilidad;

/**
 * Convierte la raíz {@code <documento>} del XML resuelto en su {@link Documento}.
 *
 * <p>No valida contra el XSD: el build ya validó el XML (y su estructura de 12 columnas) al
 * resolverlo. Aquí solo se comprueba lo que haría reventar el dibujo: un elemento desconocido o un
 * atributo con un valor imposible.
 */
public final class DefinicionDocumentoParser {

    public Documento parse(Element raiz) {
        Optional<TextoBilingue> titulo = Optional.empty();
        List<Seccion> secciones = new ArrayList<>();
        for (Element e : hijos(raiz)) {
            switch (e.getTagName()) {
                case "titulo" -> titulo = Optional.of(textos(e));
                case "seccion" -> secciones.add(seccion(e));
                default -> throw new RuntimeException("<" + e.getTagName() + "> desconocido dentro de <documento>");
            }
        }

        return new Documento(titulo, List.copyOf(secciones));
    }

    private Seccion seccion(Element e) {
        List<Fila> filas = new ArrayList<>();
        for (Element hijo : hijos(e)) {
            switch (hijo.getTagName()) {
                case "valenciano", "castellano" -> { }
                case "fila" -> filas.add(fila(hijo));
                default -> throw new RuntimeException("<" + hijo.getTagName() + "> desconocido dentro de <seccion>");
            }
        }
        return new Seccion(textos(e), List.copyOf(filas), visibilidad(e));
    }

    private Fila fila(Element e) {
        List<Celda> celdas = new ArrayList<>();
        for (Element hijo : hijos(e)) {
            celdas.add(celda(hijo));
        }
        return new Fila(List.copyOf(celdas), visibilidad(e));
    }

    private Celda celda(Element e) {
        TipoCelda tipo = switch (e.getTagName()) {
            case "campo" -> TipoCelda.CAMPO;
            case "check" -> TipoCelda.CHECK;
            case "texto" -> TipoCelda.TEXTO;
            default -> throw new RuntimeException("<" + e.getTagName() + "> desconocido dentro de <fila>");
        };
        Optional<String> nombreCampo = tipo == TipoCelda.TEXTO
                ? Optional.empty()
                : Optional.of(e.getAttribute("nombreCampo"));
        double colspan = Double.parseDouble(e.getAttribute("colspan"));
        double rowSpan = e.getAttribute("rowSpan").isEmpty() ? 1 : Double.parseDouble(e.getAttribute("rowSpan"));

        return new Celda(tipo, nombreCampo, colspan, rowSpan, textos(e), visibilidad(e), campoFirma(e));
    }
}
