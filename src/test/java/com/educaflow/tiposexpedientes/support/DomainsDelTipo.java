package com.educaflow.tiposexpedientes.support;

import com.educaflow.common.buildtools.files.tipoexpediente.TipoExpedienteInstanceFile;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Lectura del {@code domains.xml} de un tipo de expediente: cuál es <b>su</b> entidad.
 *
 * <p>La entidad del tipo es la <b>primera</b> {@code <entity>} del fichero. Un {@code domains.xml}
 * puede declarar varias (las tablas auxiliares del tipo, como las líneas de un detalle), pero solo
 * la primera es la subclase de {@code Expediente} que se tramita; el orden es el convenio, no hay
 * ninguna marca que lo diga.
 *
 * <p>El FQCN se compone con el {@code package} del {@code <module>}, que es donde Axelor genera las
 * entidades del fichero: no es el paquete de la carpeta de versión, así que la entidad no se puede
 * localizar por convención de nombre como el resto de clases del tipo.
 *
 * <p>Se parsea con JAXP sin namespaces (los tags quedan simples) y sin entidades externas, igual
 * que {@link ViewsDeFase} y que los tests de {@code com.educaflow.views}. No se usa ArchUnit:
 * ArchUnit lee bytecode, no XML.
 */
public final class DomainsDelTipo {

    /** El nombre del fichero de modelo de un tipo de expediente, en la raíz de su carpeta de versión. */
    public static final String NOMBRE_FICHERO = "domains.xml";

    private static final Map<Path, Optional<String>> cache = new HashMap<>();

    private DomainsDelTipo() {}

    /** El {@code domains.xml} del tipo, exista o no. */
    public static Path path(TipoExpedienteInstanceFile tipo) {
        return TiposExpediente.carpeta(tipo).resolve(NOMBRE_FICHERO);
    }

    /** Ruta relativa legible del {@code domains.xml} del tipo, para los mensajes de error. */
    public static String fichero(TipoExpedienteInstanceFile tipo) {
        return TiposExpediente.rel(path(tipo));
    }

    public static boolean existe(TipoExpedienteInstanceFile tipo) {
        return Files.isRegularFile(path(tipo));
    }

    /**
     * El FQCN de la entidad del tipo: la primera {@code <entity>} del {@code domains.xml}. Vacío si
     * el fichero no existe o no declara ninguna entidad; son las reglas quienes deciden si eso es
     * una violación. Cacheado.
     */
    public static synchronized Optional<String> fqcnEntidad(TipoExpedienteInstanceFile tipo) {
        Path path = path(tipo);
        Optional<String> fqcn = cache.get(path);
        if (fqcn == null) {
            fqcn = Files.isRegularFile(path) ? parse(path) : Optional.empty();
            cache.put(path, fqcn);
        }

        return fqcn;
    }

    private static Optional<String> parse(Path path) {
        Document documento = doc(path);

        Element entidad = primero(documento, "entity");
        if (entidad == null || !entidad.hasAttribute("name")) {
            return Optional.empty();
        }
        String name = entidad.getAttribute("name").trim();

        // El nombre ya cualificado gana, como en Axelor; si no, lo cualifica el package del <module>.
        if (name.contains(".")) {
            return Optional.of(name);
        }

        Element module = primero(documento, "module");
        if (module == null || !module.hasAttribute("package")) {
            return Optional.empty();
        }

        return Optional.of(module.getAttribute("package").trim() + "." + name);
    }

    /** El primer elemento con ese tag en orden de documento, o null si no hay ninguno. */
    private static Element primero(Document documento, String tag) {
        NodeList nodos = documento.getElementsByTagName(tag);

        return nodos.getLength() == 0 ? null : (Element) nodos.item(0);
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
            throw new IllegalStateException(NOMBRE_FICHERO + " no parseable: " + path + " -> " + ex.getMessage(), ex);
        }
    }
}
