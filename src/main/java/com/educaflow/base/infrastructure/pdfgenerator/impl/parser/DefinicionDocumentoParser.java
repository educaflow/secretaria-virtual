package com.educaflow.base.infrastructure.pdfgenerator.impl.parser;

import com.educaflow.base.infrastructure.pdfgenerator.impl.modelo.Celda;
import com.educaflow.base.infrastructure.pdfgenerator.impl.modelo.Documento;
import com.educaflow.base.infrastructure.pdfgenerator.impl.modelo.Fila;
import com.educaflow.base.infrastructure.pdfgenerator.impl.modelo.Seccion;
import com.educaflow.base.infrastructure.pdfgenerator.impl.modelo.TextoBilingue;
import com.educaflow.base.infrastructure.pdfgenerator.impl.modelo.TipoCelda;
import com.educaflow.base.infrastructure.pdfgenerator.impl.modelo.Visibilidad;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Convierte el XML resuelto de un documento en su {@link Documento}.
 *
 * <p>No valida contra el XSD: el build ya validó el XML (y su estructura de 12 columnas) al
 * resolverlo. Aquí solo se comprueba lo que haría reventar el dibujo: un elemento desconocido o un
 * atributo con un valor imposible.
 */
public final class DefinicionDocumentoParser {

    public Documento parse(byte[] xml) {
        Element raiz = parseDom(xml);
        if (!raiz.getTagName().equals("documento")) {
            throw new RuntimeException("El elemento raíz debe ser <documento> y es <" + raiz.getTagName() + ">");
        }

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

        return new Celda(tipo, nombreCampo, colspan, rowSpan, textos(e), visibilidad(e));
    }

    private static Visibilidad visibilidad(Element e) {
        return Visibilidad.de(e.getAttribute("visible"), e.getAttribute("siOculto"));
    }

    private static TextoBilingue textos(Element e) {
        return new TextoBilingue(textoHijo(e, "valenciano"), textoHijo(e, "castellano"));
    }

    private static String textoHijo(Element e, String nombre) {
        for (Element hijo : hijos(e)) {
            if (hijo.getTagName().equals(nombre)) {
                return hijo.getTextContent().trim();
            }
        }
        return "";
    }

    private static List<Element> hijos(Element e) {
        List<Element> hijos = new ArrayList<>();
        NodeList nodos = e.getChildNodes();
        for (int i = 0; i < nodos.getLength(); i++) {
            Node nodo = nodos.item(i);
            if (nodo instanceof Element hijo) {
                hijos.add(hijo);
            }
        }
        return hijos;
    }

    private static Element parseDom(byte[] xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            return factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml)).getDocumentElement();
        } catch (Exception ex) {
            throw new RuntimeException("El XML del documento no se puede parsear: " + ex.getMessage(), ex);
        }
    }
}
