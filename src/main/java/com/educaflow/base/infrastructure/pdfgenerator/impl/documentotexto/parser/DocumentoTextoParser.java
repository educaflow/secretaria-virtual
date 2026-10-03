package com.educaflow.base.infrastructure.pdfgenerator.impl.documentotexto.parser;

import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.dibujo.Alineacion;
import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.modelo.TextoBilingue;
import com.educaflow.base.infrastructure.pdfgenerator.impl.documentotexto.modelo.Bloque;
import com.educaflow.base.infrastructure.pdfgenerator.impl.documentotexto.modelo.DocumentoTexto;
import com.educaflow.base.infrastructure.pdfgenerator.impl.documentotexto.modelo.Espacio;
import com.educaflow.base.infrastructure.pdfgenerator.impl.documentotexto.modelo.Fila;
import com.educaflow.base.infrastructure.pdfgenerator.impl.documentotexto.modelo.Lista;
import com.educaflow.base.infrastructure.pdfgenerator.impl.documentotexto.modelo.Tabla;
import com.educaflow.base.infrastructure.pdfgenerator.impl.documentotexto.modelo.Texto;
import com.educaflow.base.infrastructure.pdfgenerator.impl.documentotexto.modelo.TipoTexto;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;

import static com.educaflow.base.infrastructure.pdfgenerator.impl.comun.parser.ElementosXml.booleano;
import static com.educaflow.base.infrastructure.pdfgenerator.impl.comun.parser.ElementosXml.campoFirma;
import static com.educaflow.base.infrastructure.pdfgenerator.impl.comun.parser.ElementosXml.hijos;
import static com.educaflow.base.infrastructure.pdfgenerator.impl.comun.parser.ElementosXml.textos;
import static com.educaflow.base.infrastructure.pdfgenerator.impl.comun.parser.ElementosXml.visibilidad;

/**
 * Convierte la raíz {@code <documentoTexto>} del XML resuelto en su {@link DocumentoTexto}.
 *
 * <p>No valida contra el XSD: el build ya validó el XML al resolverlo, y ahí también se expandieron
 * los {@code <include>}. Aquí solo se comprueba lo que haría reventar el dibujo.
 */
public final class DocumentoTextoParser {

    public DocumentoTexto parse(Element raiz) {
        TextoBilingue titulo = null;
        List<Bloque> cuerpo = new ArrayList<>();
        for (Element e : hijos(raiz)) {
            if (e.getTagName().equals("titulo")) {
                titulo = textos(e);
            } else {
                cuerpo.add(bloque(e, "<documentoTexto>"));
            }
        }
        if (titulo == null) {
            throw new RuntimeException("<documentoTexto> no lleva <titulo>, que es el de la cabecera"
                    + " de su primera página y es obligatorio");
        }
        return new DocumentoTexto(titulo, List.copyOf(cuerpo));
    }

    private Bloque bloque(Element e, String padre) {
        return switch (e.getTagName()) {
            case "parrafo" -> texto(e, TipoTexto.PARRAFO);
            case "lista" -> lista(e);
            case "espacio" -> espacio(e);
            case "tabla" -> tabla(e);
            default -> throw new RuntimeException("<" + e.getTagName() + "> desconocido dentro de " + padre);
        };
    }

    private static Texto texto(Element e, TipoTexto tipo) {
        Alineacion alineacion = Alineacion.parse(e.getAttribute("alineamiento"), tipo.getAlineacionPorDefecto());
        return new Texto(tipo, textos(e), alineacion,
                booleano(e, "negrita"), booleano(e, "mayusculas"), visibilidad(e));
    }

    private static Espacio espacio(Element e) {
        return new Espacio(Double.parseDouble(e.getAttribute("alto")), visibilidad(e), campoFirma(e));
    }

    private Lista lista(Element e) {
        List<Texto> items = hijos(e).stream()
                .map(hijo -> {
                    if (!hijo.getTagName().equals("item")) {
                        throw new RuntimeException("<" + hijo.getTagName() + "> desconocido dentro de <lista>");
                    }
                    return texto(hijo, TipoTexto.ITEM);
                })
                .toList();
        return new Lista(items, visibilidad(e));
    }

    private Tabla tabla(Element e) {
        List<Fila> filas = hijos(e).stream()
                .map(hijo -> {
                    if (!hijo.getTagName().equals("fila")) {
                        throw new RuntimeException("<" + hijo.getTagName() + "> desconocido dentro de <tabla>");
                    }
                    return fila(hijo);
                })
                .toList();
        return new Tabla(Integer.parseInt(e.getAttribute("columnas")), filas, visibilidad(e));
    }

    private Fila fila(Element e) {
        return new Fila(hijos(e).stream().map(this::celda).toList(), visibilidad(e));
    }

    private Bloque celda(Element e) {
        Bloque celda = bloque(e, "<fila>");
        if (celda instanceof Lista || celda instanceof Tabla) {
            throw new RuntimeException("<" + e.getTagName() + "> no puede ir dentro de <fila>");
        }
        return celda;
    }
}
