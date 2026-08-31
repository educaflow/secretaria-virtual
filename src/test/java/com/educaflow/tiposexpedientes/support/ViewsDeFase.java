package com.educaflow.tiposexpedientes.support;

import com.educaflow.common.buildtools.files.tipoexpediente.Fase;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lectura del {@code views.xml} de una fase: los {@code <form state="…">} de sus estados y los
 * botones de su {@code <footer>}.
 *
 * <p>Se lee el <b>fuente</b>, no el resultado del preprocesador: es lo que se escribe a mano y lo
 * que sale nombrado en los mensajes de error. El fichero es opcional para el build —un
 * {@code <object-views>} vacío no valida contra el XSD, así que una fase sin forms de estado no lo
 * tiene—, de modo que aquí una fase sin fichero da simplemente una lista vacía y son las reglas
 * quienes deciden si eso es una violación.
 *
 * <p>Se parsea con JAXP sin namespaces (los tags quedan simples) y sin entidades externas, igual
 * que los tests de {@code com.educaflow.views}. No se usa ArchUnit: ArchUnit lee bytecode, no XML.
 */
public final class ViewsDeFase {

    /** El nombre del fichero de vistas de una fase, dentro de su subcarpeta. */
    public static final String NOMBRE_FICHERO = "views.xml";

    private static final Map<Path, List<FormDeEstado>> cache = new HashMap<>();

    private ViewsDeFase() {}

    /** El {@code views.xml} de la fase, exista o no. */
    public static Path path(Fase fase) {
        return TiposExpediente.carpeta(fase).resolve(NOMBRE_FICHERO);
    }

    public static boolean existe(Fase fase) {
        return Files.isRegularFile(path(fase));
    }

    /** Ruta relativa legible del {@code views.xml} de la fase, para los mensajes de error. */
    public static String fichero(Fase fase) {
        return TiposExpediente.rel(path(fase));
    }

    /**
     * Los forms de estado del {@code views.xml} de la fase, en orden de aparición. Lista vacía si el
     * fichero no existe. Cacheado.
     */
    public static synchronized List<FormDeEstado> forms(Fase fase) {
        Path path = path(fase);
        List<FormDeEstado> forms = cache.get(path);
        if (forms == null) {
            forms = Files.isRegularFile(path) ? parse(fase, path) : List.of();
            cache.put(path, forms);
        }

        return forms;
    }

    /** Los forms de ese estado: el del perfil y el genérico, si están. */
    public static List<FormDeEstado> formsDelEstado(Fase fase, String state) {
        List<FormDeEstado> forms = new ArrayList<>();
        for (FormDeEstado form : forms(fase)) {
            if (form.state().equals(state)) {
                forms.add(form);
            }
        }

        return forms;
    }

    private static List<FormDeEstado> parse(Fase fase, Path path) {
        List<FormDeEstado> forms = new ArrayList<>();

        for (Element form : byTag(doc(path), "form")) {
            // Un views.xml de fase puede llevar además vistas Axelor normales (forms auxiliares con
            // name propio); solo son forms de estado los que llevan el atributo 'state'.
            if (!form.hasAttribute("state")) {
                continue;
            }
            forms.add(new FormDeEstado(fase, path, attr(form, "state"), attr(form, "profile"), botones(form)));
        }

        return forms;
    }

    /**
     * Los botones del {@code <footer>} del form. Solo los del footer: son los únicos que disparan
     * eventos del expediente; un {@code <button>} dentro de un panel es otra cosa.
     */
    private static List<FormDeEstado.Boton> botones(Element form) {
        List<FormDeEstado.Boton> botones = new ArrayList<>();
        for (Element footer : byTag(form, "footer")) {
            for (Element boton : byTag(footer, "button")) {
                botones.add(new FormDeEstado.Boton(attr(boton, "name"), attr(boton, "onClick")));
            }
        }

        return botones;
    }

    private static Document doc(Path path) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false); // los XML usan namespace por defecto sin prefijo
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            DocumentBuilder builder = factory.newDocumentBuilder();

            return builder.parse(path.toFile());
        } catch (Exception ex) {
            throw new IllegalStateException("views.xml no parseable: " + path + " -> " + ex.getMessage(), ex);
        }
    }

    private static List<Element> byTag(Node root, String tag) {
        List<Element> elementos = new ArrayList<>();
        NodeList nodos = (root instanceof Document documento)
                ? documento.getElementsByTagName(tag)
                : ((Element) root).getElementsByTagName(tag);
        for (int i = 0; i < nodos.getLength(); i++) {
            elementos.add((Element) nodos.item(i));
        }

        return elementos;
    }

    /** Valor del atributo sin espacios alrededor, o "" si no está. */
    private static String attr(Element elemento, String nombre) {
        return elemento.hasAttribute(nombre) ? elemento.getAttribute(nombre).trim() : "";
    }
}
