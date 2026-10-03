package com.educaflow.base.infrastructure.pdfgenerator.impl.comun.parser;

import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.modelo.TextoBilingue;
import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.modelo.Visibilidad;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ElementosXml {

    private ElementosXml() {
    }

    public static Visibilidad visibilidad(Element e) {
        return Visibilidad.de(e.getAttribute("visible"), e.getAttribute("siOculto"));
    }

    public static Optional<String> campoFirma(Element e) {
        String nombre = e.getAttribute("campoFirma");
        return nombre.isEmpty() ? Optional.empty() : Optional.of(nombre);
    }

    public static boolean booleano(Element e, String atributo) {
        String valor = e.getAttribute(atributo);
        return switch (valor) {
            case "", "false" -> false;
            case "true" -> true;
            default -> throw new RuntimeException(atributo + "=\"" + valor + "\" no es válido:"
                    + " solo admite true o false");
        };
    }

    public static TextoBilingue textos(Element e) {
        return new TextoBilingue(textoHijo(e, "valenciano"), textoHijo(e, "castellano"));
    }

    public static String textoHijo(Element e, String nombre) {
        return hijos(e).stream()
                .filter(hijo -> hijo.getTagName().equals(nombre))
                .findFirst()
                .map(hijo -> hijo.getTextContent().trim())
                .orElse("");
    }

    public static List<Element> hijos(Element e) {
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
}
