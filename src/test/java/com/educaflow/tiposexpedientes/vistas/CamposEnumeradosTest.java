package com.educaflow.tiposexpedientes.vistas;

import com.educaflow.common.buildtools.files.tipoexpediente.TipoExpedienteInstanceFile;
import com.educaflow.tiposexpedientes.support.DomainsDelTipo;
import com.educaflow.tiposexpedientes.support.EnumeradosDeDominio;
import com.educaflow.tiposexpedientes.support.TiposExpediente;
import com.educaflow.tiposexpedientes.support.Violacion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Los campos de tipo enumerado de los forms de los trámites se pintan con {@code RadioSelect} y con
 * la orientación que toca a su número de valores.
 *
 * <p>Es la misma convención que {@code VAR-6.6}…{@code VAR-6.8} de {@code agent_docs/view-rules.md}
 * fija para los sistemas y subsistemas. Los trámites están fuera del sujeto de aquel catálogo —sus
 * vistas se escriben en el formato del preprocesador—, así que aquí se comprueba sobre sus fuentes:
 *
 * <ul>
 *   <li>los {@code views.xml} de cada tipo de expediente (el de la raíz de la versión, con el form
 *       plantilla y los forms auxiliares, y el de cada fase);</li>
 *   <li>los demás ficheros de vistas que cuelgan de la carpeta de trámites sin ser de ningún tipo
 *       (el de los paneles comunes), cuyos paneles se copian a los forms de <b>todos</b> los tipos.</li>
 * </ul>
 *
 * <p>De qué entidad son los campos de un {@code <form>}:
 *
 * <ul>
 *   <li>si lleva {@code model}, de esa entidad;</li>
 *   <li>si no lo lleva (un {@code <form state="…">}), de la entidad del tipo;</li>
 *   <li>y en un fichero común, además, de la entidad de cada tipo: un campo como
 *       {@code resultadoVerificacion} no es de {@code Expediente}, lo declara cada tipo.</li>
 * </ul>
 *
 * <p>Los campos de un {@code <editor>} son campos como cualquier otro, pero de otra entidad: la que
 * relaciona el campo dueño del editor. Y los ocultos ({@code hidden="true"}, {@code showIf="false"})
 * cuentan igual que los visibles: mañana pueden dejar de estar ocultos.
 *
 * <p>Los de solo lectura no tienen opciones que elegir, así que el {@code RadioSelect} no se les
 * exige (W2); si lo llevan, la orientación sí (W3). Un campo es de solo lectura si:
 *
 * <ul>
 *   <li>lleva {@code readonly="true"} él o alguno de sus ancestros hasta el {@code <form>}
 *       inclusive (un {@code <panel readonly="true">});</li>
 *   <li>o el panel de la plantilla en el que está se incluye desde algún {@code <include-panels>}
 *       y <b>siempre</b> con el prefijo {@code -} (que pone readonly todos sus fields). Se miran los
 *       {@code <include-panels>} del tipo del fichero, o los de todos los tipos si es común. Un
 *       panel que no se incluye nunca no se considera de solo lectura.</li>
 * </ul>
 *
 * <p>No son campos del form, y no se miran: los {@code <field>} hijos de un {@code <panel-related>}
 * (son columnas de su rejilla). Tampoco los de un {@code <grid>}.
 *
 * <p><b>Estos tests se escriben A MANO.</b> No son una proyección de ningún catálogo markdown, al
 * contrario que {@code com.educaflow.architecture} y {@code com.educaflow.views}: este fichero es la
 * fuente de verdad y se edita directamente.
 */
class CamposEnumeradosTest {

    /** Hasta este número de valores el enumerado se pinta en horizontal; con más, en vertical. */
    private static final int MAXIMO_VALORES_EN_HORIZONTAL = 4;

    // -----------------------------------------------------------------------------------------
    // W1 — ningún campo usa SwitchSelect
    // -----------------------------------------------------------------------------------------

    @Test
    @DisplayName("W1: ningún <field> de un <form> usa widget=\"SwitchSelect\"")
    void w1_ningunCampoUsaSwitchSelect() {
        List<Violacion> violaciones = new ArrayList<>();

        for (FicheroDeVistas fichero : ficherosDeVistas()) {
            for (Element form : byTag(fichero.documento(), "form")) {
                for (Element field : byTag(form, "field")) {
                    String widget = attr(field, "widget");
                    if ("switchselect".equals(widget.replace("-", "").toLowerCase(Locale.ROOT))) {
                        violaciones.add(new Violacion(fichero.tipo(), fichero.rel(),
                                "el <field name=\"" + attr(field, "name") + "\"> de " + nombre(form)
                                + " usa widget=\"" + widget + "\""));
                    }
                }
            }
        }

        Violacion.assertNone("[W1] Ningún <field> de un <form> puede usar widget=\"SwitchSelect\":"
                + " la elección entre una lista de opciones se pinta siempre con RadioSelect.", violaciones);
    }

    // -----------------------------------------------------------------------------------------
    // W2 — los campos de tipo enumerado usan RadioSelect
    // -----------------------------------------------------------------------------------------

    @Test
    @DisplayName("W2: todo <field> editable de un <form> cuyo tipo es un enumerado lleva widget=\"RadioSelect\"")
    void w2_losCamposEnumeradosUsanRadioSelect() {
        List<Violacion> violaciones = new ArrayList<>();

        for (CampoEnumerado campo : camposEnumerados()) {
            if ("RadioSelect".equals(attr(campo.field(), "widget")) || esDeSoloLectura(campo)) {
                continue;
            }

            violaciones.add(new Violacion(campo.fichero().tipo(), campo.fichero().rel(),
                    "el <field name=\"" + campo.name() + "\"> de " + nombre(campo.form())
                    + " es de tipo enumerado y "
                    + (campo.field().hasAttribute("widget")
                            ? ("lleva widget=\"" + attr(campo.field(), "widget") + "\"")
                            : "no lleva widget")
                    + ": debe llevar widget=\"RadioSelect\" x-direction=\""
                    + direccionEsperada(campo.valores()) + "\""));
        }

        Violacion.assertNone("[W2] Todo <field> editable de un <form> cuyo tipo en el modelo es un enumerado debe"
                + " llevar widget=\"RadioSelect\"; solo los de solo lectura (readonly=\"true\" en el campo o en un"
                + " ancestro, o panel incluido siempre con '-') quedan libres.", violaciones);
    }

    // -----------------------------------------------------------------------------------------
    // W3 — la orientación depende del número de valores
    // -----------------------------------------------------------------------------------------

    @Test
    @DisplayName("W3: el RadioSelect de un enumerado lleva x-direction horizontal hasta 4 valores y vertical con 5 o más")
    void w3_laOrientacionDependeDelNumeroDeValores() {
        List<Violacion> violaciones = new ArrayList<>();

        for (CampoEnumerado campo : camposEnumerados()) {
            // Sin RadioSelect ya lo dice W2, que trae la orientación que le toca.
            if (!"RadioSelect".equals(attr(campo.field(), "widget"))) {
                continue;
            }
            if (campo.valores() < 0) {
                violaciones.add(new Violacion(campo.fichero().tipo(), campo.fichero().rel(),
                        "no se encuentra en los dominios el enumerado del campo '" + campo.name()
                        + "' de " + campo.entidad() + ", así que no se sabe cuántos valores tiene"));
                continue;
            }

            String esperada = direccionEsperada(campo.valores());
            String actual = attr(campo.field(), "x-direction");
            if (esperada.equals(actual)) {
                continue;
            }

            violaciones.add(new Violacion(campo.fichero().tipo(), campo.fichero().rel(),
                    "el <field name=\"" + campo.name() + "\"> de " + nombre(campo.form()) + " "
                    + (actual.isEmpty() ? "no lleva x-direction" : ("lleva x-direction=\"" + actual + "\""))
                    + ": su enumerado tiene " + campo.valores() + " valores, así que debe llevar"
                    + " x-direction=\"" + esperada + "\""));
        }

        Violacion.assertNone("[W3] El RadioSelect de un campo de tipo enumerado debe llevar x-direction:"
                + " \"horizontal\" si el enumerado tiene hasta " + MAXIMO_VALORES_EN_HORIZONTAL
                + " valores y \"vertical\" si tiene más.", violaciones);
    }

    // -----------------------------------------------------------------------------------------
    // Ayudas
    // -----------------------------------------------------------------------------------------

    /**
     * Un fichero de vistas de la carpeta de trámites. {@code tipo} es el tipo de expediente al que
     * pertenece, o {@code null} si es común a todos.
     */
    private record FicheroDeVistas(Path path, Document documento, TipoExpedienteInstanceFile tipoExpediente) {

        String rel() {
            return TiposExpediente.rel(path);
        }

        /** Cómo se identifica en los mensajes de error. */
        String tipo() {
            return (tipoExpediente == null) ? "comunes" : tipoExpediente.getCode();
        }
    }

    /** Un {@code <field>} de un form que es de tipo enumerado en {@code entidad}, con sus {@code valores}. */
    private record CampoEnumerado(FicheroDeVistas fichero, Element form, Element field, String entidad, int valores) {

        String name() {
            return attr(field, "name");
        }
    }

    private static String direccionEsperada(int valores) {
        return (valores <= MAXIMO_VALORES_EN_HORIZONTAL) ? "horizontal" : "vertical";
    }

    /** Si el campo es de solo lectura (ver el javadoc de la clase). */
    private static boolean esDeSoloLectura(CampoEnumerado campo) {
        Element panelDePlantilla = null;
        for (Node nodo = campo.field(); nodo instanceof Element elemento; nodo = nodo.getParentNode()) {
            if ("true".equals(attr(elemento, "readonly"))) {
                return true;
            }
            if (elemento == campo.form()) {
                break;
            }
            if (elemento.getParentNode() == campo.form()) {
                panelDePlantilla = elemento;
            }
        }

        return (panelDePlantilla != null) && campo.form().hasAttribute("name")
                && incluidoSiempreReadonly(campo.fichero().tipoExpediente(), attr(panelDePlantilla, "name"));
    }

    /**
     * Si el panel {@code nombre} se incluye desde algún {@code <include-panels>} y siempre con el
     * prefijo {@code -}. Se miran los de los ficheros de {@code tipo}, o los de todos si es {@code null}.
     */
    private static boolean incluidoSiempreReadonly(TipoExpedienteInstanceFile tipo, String nombre) {
        boolean incluido = false;
        for (FicheroDeVistas fichero : ficherosDeVistas()) {
            if ((tipo != null) && !tipo.getCode().equals(fichero.tipo())) {
                continue;
            }
            for (Element includePanels : byTag(fichero.documento(), "include-panels")) {
                for (String linea : includePanels.getTextContent().trim().split("\\s+")) {
                    if (linea.equals(nombre)) {
                        return false;
                    }
                    if (linea.equals("-" + nombre)) {
                        incluido = true;
                    }
                }
            }
        }

        return incluido;
    }

    /**
     * Los campos de tipo enumerado de todos los forms. En un fichero común un mismo {@code <field>}
     * sale una vez por cada entidad que lo declara como enumerado con un número de valores distinto:
     * el panel se copia a los forms de todos los tipos y tiene que valer para todos.
     */
    private static List<CampoEnumerado> camposEnumerados() {
        List<CampoEnumerado> campos = new ArrayList<>();

        for (FicheroDeVistas fichero : ficherosDeVistas()) {
            for (Element form : byTag(fichero.documento(), "form")) {
                for (Element field : byTag(form, "field")) {
                    if ("panel-related".equals(field.getParentNode().getNodeName())) {
                        continue;
                    }

                    Set<Integer> vistos = new LinkedHashSet<>();
                    for (String entidad : entidades(fichero, form, field)) {
                        Integer valores = EnumeradosDeDominio.campos(entidad).get(attr(field, "name"));
                        if ((valores != null) && vistos.add(valores)) {
                            campos.add(new CampoEnumerado(fichero, form, field, entidad, valores));
                        }
                    }
                }
            }
        }

        return campos;
    }

    /**
     * Las entidades de las que puede ser el campo: las del form (ver el javadoc de la clase) o, dentro
     * de un {@code <editor>}, las que relaciona el campo dueño del editor.
     */
    private static Set<String> entidades(FicheroDeVistas fichero, Element form, Element field) {
        for (Node nodo = field.getParentNode(); nodo instanceof Element ancestro; nodo = nodo.getParentNode()) {
            if ("editor".equals(ancestro.getNodeName()) && (ancestro.getParentNode() instanceof Element duenyo)) {
                Set<String> relacionadas = new LinkedHashSet<>();
                for (String entidad : entidades(fichero, form, duenyo)) {
                    String relacionada = EnumeradosDeDominio.entidadRelacionada(entidad, attr(duenyo, "name"));
                    if (relacionada != null) {
                        relacionadas.add(relacionada);
                    }
                }

                return relacionadas;
            }
        }

        Set<String> entidades = new LinkedHashSet<>();
        if (form.hasAttribute("model")) {
            entidades.add(attr(form, "model"));
        } else if (fichero.tipoExpediente() != null) {
            DomainsDelTipo.fqcnEntidad(fichero.tipoExpediente()).ifPresent(entidades::add);
        }
        if (fichero.tipoExpediente() == null) {
            for (TipoExpedienteInstanceFile tipo : TiposExpediente.all()) {
                DomainsDelTipo.fqcnEntidad(tipo).ifPresent(entidades::add);
            }
        }

        return entidades;
    }

    /** Cómo se identifica un form en los mensajes de error: por su name o, si es de estado, por su estado y perfil. */
    private static String nombre(Element form) {
        if (form.hasAttribute("name")) {
            return "<form name=\"" + attr(form, "name") + "\">";
        }

        return "<form state=\"" + attr(form, "state") + "\""
                + (form.hasAttribute("profile") ? (" profile=\"" + attr(form, "profile") + "\"") : "") + ">";
    }

    private static List<FicheroDeVistas> ficherosDeVistas;

    /** Todos los XML de la carpeta de trámites cuyo elemento raíz es {@code object-views}. */
    private static synchronized List<FicheroDeVistas> ficherosDeVistas() {
        if (ficherosDeVistas == null) {
            ficherosDeVistas = leerFicherosDeVistas();
        }

        return ficherosDeVistas;
    }

    private static List<FicheroDeVistas> leerFicherosDeVistas() {
        Map<Path, TipoExpedienteInstanceFile> carpetasDeTipo = new LinkedHashMap<>();
        for (TipoExpedienteInstanceFile tipo : TiposExpediente.all()) {
            carpetasDeTipo.put(TiposExpediente.carpeta(tipo).toAbsolutePath().normalize(), tipo);
        }

        List<Path> paths;
        try (Stream<Path> walk = Files.walk(TiposExpediente.raizDeTramites())) {
            paths = walk
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".xml"))
                    .sorted()
                    .toList();
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudieron listar los XML de la carpeta de trámites", ex);
        }

        List<FicheroDeVistas> ficheros = new ArrayList<>();
        for (Path path : paths) {
            Document documento = doc(path);
            if (!"object-views".equals(documento.getDocumentElement().getNodeName())) {
                continue;
            }

            TipoExpedienteInstanceFile tipo = null;
            for (Map.Entry<Path, TipoExpedienteInstanceFile> carpeta : carpetasDeTipo.entrySet()) {
                if (path.toAbsolutePath().normalize().startsWith(carpeta.getKey())) {
                    tipo = carpeta.getValue();
                }
            }
            ficheros.add(new FicheroDeVistas(path, documento, tipo));
        }

        return ficheros;
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
            throw new IllegalStateException("XML no parseable: " + path + " -> " + ex.getMessage(), ex);
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
