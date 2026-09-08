package com.educaflow.views.support;

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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Descubrimiento y parseo de los XML de vistas Axelor para los tests de "arquitectura de vistas"
 * (ver agent_docs/view-rules.md). NO usa ArchUnit: ArchUnit analiza bytecode, no XML.
 *
 * <p>El discriminador de fichero de vista es la carpeta {@code views/} bajo
 * {@code src/main/java/com/educaflow}, NO el elemento raíz (object-views aparece también en
 * menus.xml, DefaultModelController.xml y las views.xml de los tipos de expediente).
 *
 * <p>Se excluyen los paquetes exentos ({@code gestioncentro}, {@code expedientes},
 * {@code tramites}), con la misma política que
 * agent_docs/view-rules.md (§ Paquetes exentos).
 */
public final class ViewFiles {

    /**
     * Segmentos de ruta exentos. Las reglas de vistas aplican a TODOS los sistemas y subsistemas
     * EXCEPTO {@code gestioncentro}, {@code expedientes} y {@code tramites}
     * (framework propio de expediente/tramitación y pantallas de gestión de centro), que quedan fuera
     * del sujeto de todas las reglas. Debe coincidir con la lista de agent_docs/view-rules.md.
     */
    private static final List<String> PAQUETES_EXENTOS =
            List.of("gestioncentro", "expedientes", "tramites");

    private static List<ViewFile> cache;
    private static Document menusCache;
    private static Document hideMenusCache;
    private static final Map<Path, Document> docCache = new HashMap<>();

    private ViewFiles() {}

    /** Todos los ficheros de vistas NO exentos, parseados. Cacheado. */
    public static synchronized List<ViewFile> all() {
        if (cache == null) {
            cache = load();
        }
        return cache;
    }

    private static List<ViewFile> load() {
        Path base = projectRoot().resolve("src/main/java/com/educaflow");
        List<ViewFile> result = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(base)) {
            List<Path> xmls = walk
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".xml"))
                    .filter(p -> p.getParent() != null && p.getParent().getFileName().toString().equals("views"))
                    .filter(ViewFiles::notExempt)
                    .sorted()
                    .toList();
            for (Path p : xmls) {
                result.add(new ViewFile(p, parse(p)));
            }
        } catch (IOException e) {
            throw new IllegalStateException("No se pudieron listar los XML de vistas bajo " + base, e);
        }
        if (result.isEmpty()) {
            throw new IllegalStateException("No se encontró ningún XML de vistas bajo " + base
                    + " (¿directorio de trabajo incorrecto?)");
        }
        return result;
    }

    private static boolean notExempt(Path p) {
        String s = p.toString().replace('\\', '/');
        for (String ex : PAQUETES_EXENTOS) {
            if (s.contains("/" + ex + "/")) {
                return false;
            }
        }
        return true;
    }

    /** Localiza la raíz del proyecto subiendo desde el directorio de trabajo hasta hallar src/main/java. */
    private static Path projectRoot() {
        Path dir = Path.of("").toAbsolutePath();
        for (Path p = dir; p != null; p = p.getParent()) {
            if (Files.isDirectory(p.resolve("src/main/java/com/educaflow"))) {
                return p;
            }
        }
        return dir;
    }

    private static Document parse(Path p) {
        try {
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            f.setNamespaceAware(false); // los XML usan namespace por defecto sin prefijo; así los tags quedan simples
            f.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            f.setFeature("http://xml.org/sax/features/external-general-entities", false);
            f.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            DocumentBuilder b = f.newDocumentBuilder();
            return b.parse(p.toFile());
        } catch (Exception e) {
            throw new IllegalStateException("XML de vista no parseable: " + p + " -> " + e.getMessage(), e);
        }
    }

    /** Ruta del fichero único de menús (también forma parte del ámbito de análisis). */
    public static Path menusPath() {
        return projectRoot().resolve("src/main/java/com/educaflow/secretariavirtual/menus/menus.xml");
    }

    /** Documento parseado de menus.xml. */
    public static synchronized Document menusDoc() {
        if (menusCache == null) {
            menusCache = parse(menusPath());
        }
        return menusCache;
    }

    /** Líneas de texto crudo de menus.xml (para las reglas de formato de la Categoría 10). */
    public static List<String> menusLineas() {
        return lineas(menusPath());
    }

    /**
     * Ruta del fichero de ocultaciones de menús de Axelor (también forma parte del ámbito de
     * análisis). Junto con {@link #menusPath()} son los dos únicos ficheros con {@code <menuitem>}.
     */
    public static Path hideMenusPath() {
        return projectRoot().resolve("src/main/java/com/educaflow/secretariavirtual/menus/hide-menus.xml");
    }

    /** Documento parseado de hide-menus.xml. */
    public static synchronized Document hideMenusDoc() {
        if (hideMenusCache == null) {
            hideMenusCache = parse(hideMenusPath());
        }
        return hideMenusCache;
    }

    /** Líneas de texto crudo de hide-menus.xml (para las reglas de formato de la Categoría 10). */
    public static List<String> hideMenusLineas() {
        return lineas(hideMenusPath());
    }

    private static List<String> lineas(Path p) {
        try {
            return Files.readAllLines(p);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer " + p, e);
        }
    }

    /**
     * Todos los XML de {@code src/main/java/com/educaflow} cuyo elemento raíz es {@code object-views},
     * SIN aplicar la exención de paquetes y SIN exigir que estén bajo {@code views/}.
     *
     * <p>El build copia todo XML de {@code src/main/java} bajo {@code build/resources/main/views/},
     * así que Axelor carga cualquiera de ellos viva donde viva: para VAR-1.3 el ámbito es el árbol
     * entero, no solo las carpetas {@code views/}. Los XML que no son object-views (dominios,
     * data-init, documentos PDF…) se descartan sin parsear a fondo.
     */
    public static List<Path> todosLosObjectViews() {
        Path base = projectRoot().resolve("src/main/java/com/educaflow");
        try (Stream<Path> walk = Files.walk(base)) {
            return walk
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".xml"))
                    .filter(ViewFiles::esObjectViews)
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudieron listar los XML bajo " + base, e);
        }
    }

    /** true si el XML parsea y su elemento raíz es {@code object-views}. */
    private static boolean esObjectViews(Path p) {
        try {
            return "object-views".equals(parseDoc(p).getDocumentElement().getNodeName());
        } catch (RuntimeException e) {
            return false; // no parsea: no es un fichero de vistas, no es sujeto de estas reglas
        }
    }

    /** Documento parseado de un XML cualquiera del árbol, cacheado por ruta. */
    public static synchronized Document parseDoc(Path p) {
        return docCache.computeIfAbsent(p, ViewFiles::parse);
    }

    /**
     * Nombres de entidad declarados en los XML de {@code ../domains} del módulo dueño del fichero
     * de vistas (elementos {@code <entity name="…">}).
     */
    public static Set<String> entidadesDelModulo(ViewFile vf) {
        Path domains = vf.path().getParent().resolveSibling("domains");
        Set<String> entidades = new HashSet<>();
        if (!Files.isDirectory(domains)) {
            return entidades;
        }
        try (Stream<Path> s = Files.list(domains)) {
            for (Path p : s.filter(x -> x.getFileName().toString().endsWith(".xml")).toList()) {
                for (Element e : byTag(parse(p), "entity")) {
                    entidades.add(attr(e, "name"));
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo listar " + domains, e);
        }
        return entidades;
    }

    // ---- helpers DOM estáticos (sin namespace) ----

    /** Todos los elementos con ese tag bajo el nodo (recursivo). */
    public static List<Element> byTag(Node root, String tag) {
        List<Element> out = new ArrayList<>();
        NodeList nl = (root instanceof Document d) ? d.getElementsByTagName(tag)
                : ((Element) root).getElementsByTagName(tag);
        for (int i = 0; i < nl.getLength(); i++) {
            out.add((Element) nl.item(i));
        }
        return out;
    }

    /** Hijos directos con ese tag. */
    public static List<Element> childrenByTag(Node parent, String tag) {
        List<Element> out = new ArrayList<>();
        NodeList nl = parent.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node n = nl.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE && n.getNodeName().equals(tag)) {
                out.add((Element) n);
            }
        }
        return out;
    }

    /** Valor del atributo, o "" si no está. */
    public static String attr(Element e, String name) {
        return e.hasAttribute(name) ? e.getAttribute(name) : "";
    }

    public static boolean hasAttr(Element e, String name) {
        return e.hasAttribute(name);
    }
}
