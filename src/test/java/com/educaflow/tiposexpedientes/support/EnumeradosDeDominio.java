package com.educaflow.tiposexpedientes.support;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Los campos de tipo enumerado de cada entidad del proyecto y cuántos valores tiene su enumerado,
 * leídos de los XML de dominio: los {@code domains/*.xml} de los sistemas y subsistemas y el
 * {@code domains.xml} de cada tipo de expediente.
 *
 * <p>Un campo es de tipo enumerado si su {@code <entity>} lo declara con un hijo
 * {@code <enum name="…" ref="…">}; el {@code ref} apunta al {@code <enum>} de nivel superior que
 * lista los valores como {@code <item>}. Un {@code ref} sin punto es un enumerado del mismo
 * {@code package} de {@code <module>} que la entidad; con punto, es ya su nombre completo.
 *
 * <p>Los campos heredados cuentan: la entidad de un tipo de expediente {@code extends="Expediente"}
 * y sus forms pintan también los campos de {@code Expediente}.
 *
 * <p>Se parsea con JAXP sin namespaces y sin entidades externas, igual que {@link ViewsDeFase}.
 */
public final class EnumeradosDeDominio {

    /** Una {@code <entity>}: de quién hereda, el {@code ref} (ya cualificado) de cada campo enumerado y el de cada campo relacional. */
    private record Entidad(String padre, Map<String, String> refPorCampo, Map<String, String> entidadPorRelacion) {}

    /** Tags de dominio de los campos relacionales, cuyo {@code ref} es la entidad relacionada. */
    private static final List<String> TAGS_RELACION =
            List.of("many-to-one", "one-to-one", "one-to-many", "many-to-many");

    private static Map<String, Entidad> entidades;
    private static Map<String, Integer> valoresPorEnumerado;

    private EnumeradosDeDominio() {}

    /**
     * Los campos de tipo enumerado de la entidad, propios y heredados, cada uno con el número de
     * valores de su enumerado ({@code -1} si el {@code ref} no apunta a ningún {@code <enum>} de los
     * dominios). Vacío si ningún dominio declara la entidad.
     */
    public static synchronized Map<String, Integer> campos(String fqcnEntidad) {
        if (entidades == null) {
            load();
        }

        Map<String, Integer> campos = new LinkedHashMap<>();
        // El límite corta un ciclo de extends mal escrito en vez de colgar el test.
        String actual = fqcnEntidad;
        for (int nivel = 0; (nivel < 10) && (actual != null) && entidades.containsKey(actual); nivel++) {
            Entidad entidad = entidades.get(actual);
            for (Map.Entry<String, String> campo : entidad.refPorCampo().entrySet()) {
                campos.putIfAbsent(campo.getKey(), valoresPorEnumerado.getOrDefault(campo.getValue(), -1));
            }
            actual = entidad.padre();
        }

        return campos;
    }

    /**
     * El FQCN de la entidad a la que apunta el campo relacional de la entidad, propio o heredado, o
     * {@code null} si no es un campo relacional de sus dominios. Es la entidad de los campos de un
     * {@code <editor>}.
     */
    public static synchronized String entidadRelacionada(String fqcnEntidad, String campo) {
        if (entidades == null) {
            load();
        }

        String actual = fqcnEntidad;
        for (int nivel = 0; (nivel < 10) && (actual != null) && entidades.containsKey(actual); nivel++) {
            Entidad entidad = entidades.get(actual);
            if (entidad.entidadPorRelacion().containsKey(campo)) {
                return entidad.entidadPorRelacion().get(campo);
            }
            actual = entidad.padre();
        }

        return null;
    }

    private static void load() {
        entidades = new HashMap<>();
        valoresPorEnumerado = new HashMap<>();

        for (Path path : ficherosDeDominio()) {
            Document documento = doc(path);
            Element raiz = documento.getDocumentElement();
            if (!"domain-models".equals(raiz.getNodeName())) {
                continue;
            }
            List<Element> modules = hijos(raiz, "module");
            if (modules.isEmpty()) {
                continue;
            }
            String paquete = modules.get(0).getAttribute("package").trim();

            for (Element enumerado : hijos(raiz, "enum")) {
                valoresPorEnumerado.put(cualificar(paquete, enumerado.getAttribute("name")),
                        hijos(enumerado, "item").size());
            }
            for (Element entity : hijos(raiz, "entity")) {
                String padre = entity.hasAttribute("extends")
                        ? cualificar(paquete, entity.getAttribute("extends")) : null;
                Entidad entidad = entidades.computeIfAbsent(cualificar(paquete, entity.getAttribute("name")),
                        clave -> new Entidad(padre, new LinkedHashMap<>(), new LinkedHashMap<>()));
                for (Element campo : hijos(entity, "enum")) {
                    entidad.refPorCampo().put(campo.getAttribute("name").trim(),
                            cualificar(paquete, campo.getAttribute("ref")));
                }
                for (String tag : TAGS_RELACION) {
                    for (Element relacion : hijos(entity, tag)) {
                        entidad.entidadPorRelacion().put(relacion.getAttribute("name").trim(),
                                cualificar(paquete, relacion.getAttribute("ref")));
                    }
                }
            }
        }
    }

    /** El nombre ya cualificado gana, como en Axelor; si no, lo cualifica el package del {@code <module>}. */
    private static String cualificar(String paquete, String nombre) {
        String limpio = nombre.trim();

        return limpio.contains(".") ? limpio : (paquete + "." + limpio);
    }

    /** Los {@code domains/*.xml} de todo el árbol y los {@code domains.xml} de los tipos de expediente. */
    private static List<Path> ficherosDeDominio() {
        try (Stream<Path> walk = Files.walk(TiposExpediente.origen())) {
            return walk
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".xml"))
                    .filter(path -> DomainsDelTipo.NOMBRE_FICHERO.equals(path.getFileName().toString())
                            || "domains".equals(path.getParent().getFileName().toString()))
                    .sorted()
                    .toList();
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudieron listar los XML de dominio", ex);
        }
    }

    private static List<Element> hijos(Element padre, String tag) {
        List<Element> hijos = new ArrayList<>();
        NodeList nodos = padre.getChildNodes();
        for (int i = 0; i < nodos.getLength(); i++) {
            Node nodo = nodos.item(i);
            if ((nodo instanceof Element elemento) && elemento.getNodeName().equals(tag)) {
                hijos.add(elemento);
            }
        }

        return hijos;
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
            throw new IllegalStateException("XML de dominio no parseable: " + path + " -> " + ex.getMessage(), ex);
        }
    }
}
