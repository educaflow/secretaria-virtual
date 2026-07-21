// =====================================================================
// GENERADO por /create-view-test desde agent_docs/view-rules.md
// NO EDITAR A MANO. Para cambiar un test, edita view-rules.md (o corrige
// la traducción en el skill /create-view-test) y vuelve a ejecutarlo.
// =====================================================================
package com.educaflow.views.integridad;

import com.educaflow.views.support.Index;
import com.educaflow.views.support.ViewFile;
import com.educaflow.views.support.ViewFiles;
import com.educaflow.views.support.Violacion;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Categoría 4 de agent_docs/view-rules.md — Integridad referencial (cross-XML global).
 *
 * <p><b>Sujeto</b> de las dos reglas: los ficheros NO exentos ({@link ViewFiles#all()}) más
 * {@code menus.xml}. <b>Ámbito de resolución</b> (dónde puede vivir lo declarado/lo que
 * referencia): TODOS los {@code views/*.xml} del proyecto, incluidos los paquetes exentos, más
 * {@code menus.xml} — los paquetes exentos quedan fuera del sujeto de las reglas, pero
 * {@code menus.xml} abre action-view declarados en ellos (gestioncentro, expedientes, tramites)
 * y una acción no exenta puede estar referenciada desde un fichero exento.
 */
class Categoria4IntegridadTest {

    /** Eventos de la tabla de referencias de VAR-4.1. */
    private static final List<String> EVENTOS =
            List.of("onNew", "onLoad", "onSave", "onChange", "onSelect", "onClick");

    /** Elementos que pueden llevar esos eventos, según la tabla de VAR-4.1. */
    private static final List<String> TAGS_CON_EVENTOS =
            List.of("form", "field", "button", "panel", "panel-related");

    /** Tipos de acción declarable con '@' — el sujeto de VAR-4.2. */
    private static final List<String> TAGS_ACCION = List.of("action-view", "action-group",
            "action-method", "action-script", "action-record", "action-attrs",
            "action-validate", "action-condition");

    // ------------------------------------------------------------------
    // Ámbito de resolución (todos los views/ + menus.xml) e índices, cacheados
    // ------------------------------------------------------------------

    /** Nombres declarados en todo el ámbito, agrupados por lo que exige la tabla de VAR-4.1. */
    private record Declarados(Set<String> grids, Set<String> forms, Set<String> actionViews,
                              Set<String> actionGroups, Set<String> actionMethods,
                              Set<String> acciones /* las 7 acciones que no son action-view */) {}

    private static List<Document> docsAmbitoCache;
    private static Declarados declaradosCache;
    private static Set<String> referenciadosCache;
    private static String fuentesJavaCache;

    /** Raíz {@code src/main/java/com/educaflow}, deducida de la ruta de un fichero de vistas. */
    private static Path baseEducaflow() {
        Path p = ViewFiles.all().get(0).path().toAbsolutePath();
        while (p != null) {
            if (p.getFileName() != null && "educaflow".equals(p.getFileName().toString())
                    && p.getParent() != null && p.getParent().getFileName() != null
                    && "com".equals(p.getParent().getFileName().toString())) {
                return p;
            }
            p = p.getParent();
        }
        throw new IllegalStateException("No se pudo localizar com/educaflow desde los ficheros de vistas");
    }

    /** Todos los documentos del ámbito de resolución: views/*.xml (incluidos exentos) + menus.xml. */
    private static synchronized List<Document> docsAmbito() {
        if (docsAmbitoCache == null) {
            List<Document> docs = new ArrayList<>();
            Set<Path> yaParseados = new HashSet<>();
            for (ViewFile vf : ViewFiles.all()) { // reutiliza los ya parseados por el soporte
                docs.add(vf.doc());
                yaParseados.add(vf.path().toAbsolutePath().normalize());
            }
            try (Stream<Path> walk = Files.walk(baseEducaflow())) {
                for (Path p : walk.filter(Files::isRegularFile)
                        .filter(x -> x.getFileName().toString().endsWith(".xml"))
                        .filter(x -> x.getParent() != null
                                && "views".equals(x.getParent().getFileName().toString()))
                        .sorted().toList()) {
                    if (!yaParseados.contains(p.toAbsolutePath().normalize())) {
                        docs.add(parse(p)); // fichero de un paquete exento
                    }
                }
            } catch (IOException e) {
                throw new IllegalStateException("No se pudieron listar los views/*.xml del ámbito", e);
            }
            docs.add(ViewFiles.menusDoc());
            docsAmbitoCache = docs;
        }
        return docsAmbitoCache;
    }

    /** Parseo con la misma configuración segura y sin namespaces que el soporte. */
    private static Document parse(Path p) {
        try {
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            f.setNamespaceAware(false);
            f.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            f.setFeature("http://xml.org/sax/features/external-general-entities", false);
            f.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            return f.newDocumentBuilder().parse(p.toFile());
        } catch (Exception e) {
            throw new IllegalStateException("XML de vista no parseable: " + p + " -> " + e.getMessage(), e);
        }
    }

    private static synchronized Declarados declarados() {
        if (declaradosCache == null) {
            Set<String> grids = new HashSet<>();
            Set<String> forms = new HashSet<>();
            Set<String> actionViews = new HashSet<>();
            Set<String> actionGroups = new HashSet<>();
            Set<String> actionMethods = new HashSet<>();
            Set<String> acciones = new HashSet<>();
            for (Document doc : docsAmbito()) {
                recogerNombres(doc, "grid", grids);
                recogerNombres(doc, "form", forms);
                recogerNombres(doc, "action-view", actionViews);
                for (String tag : List.of("action-group", "action-method", "action-record",
                        "action-validate", "action-condition", "action-script", "action-attrs")) {
                    recogerNombres(doc, tag, acciones);
                }
                recogerNombres(doc, "action-group", actionGroups);
                recogerNombres(doc, "action-method", actionMethods);
            }
            declaradosCache = new Declarados(grids, forms, actionViews, actionGroups, actionMethods, acciones);
        }
        return declaradosCache;
    }

    private static void recogerNombres(Document doc, String tag, Set<String> destino) {
        for (Element e : ViewFiles.byTag(doc, tag)) {
            String name = ViewFiles.attr(e, "name");
            if (!name.isBlank()) {
                destino.add(name);
            }
        }
    }

    /**
     * Todos los nombres referenciados desde algún sitio de la tabla de VAR-4.1, recorriendo TODO
     * el ámbito (también los ficheros exentos: una acción no exenta referenciada solo desde un
     * fichero exento no es huérfana).
     */
    private static synchronized Set<String> referenciados() {
        if (referenciadosCache == null) {
            Set<String> r = new HashSet<>();
            for (Document doc : docsAmbito()) {
                // grid-view / form-view de field y panel-related
                for (String tag : List.of("field", "panel-related")) {
                    for (Element e : ViewFiles.byTag(doc, tag)) {
                        anhadirNoVacio(r, ViewFiles.attr(e, "grid-view"));
                        anhadirNoVacio(r, ViewFiles.attr(e, "form-view"));
                    }
                }
                // <view type="grid|form" name="…"> de action-view
                for (Element av : ViewFiles.byTag(doc, "action-view")) {
                    for (Element view : ViewFiles.childrenByTag(av, "view")) {
                        String type = ViewFiles.attr(view, "type");
                        if ("grid".equals(type) || "form".equals(type)) {
                            anhadirNoVacio(r, ViewFiles.attr(view, "name"));
                        }
                    }
                }
                // action de <menuitem> (solo aparece en menus.xml)
                for (Element mi : ViewFiles.byTag(doc, "menuitem")) {
                    anhadirNoVacio(r, ViewFiles.attr(mi, "action"));
                }
                // action de <panel-dashlet>
                for (Element pd : ViewFiles.byTag(doc, "panel-dashlet")) {
                    anhadirNoVacio(r, ViewFiles.attr(pd, "action"));
                }
                // expr="action:{name}" de <field>
                for (Element f : ViewFiles.byTag(doc, "field")) {
                    String expr = ViewFiles.attr(f, "expr");
                    if (expr.startsWith("action:")) {
                        anhadirNoVacio(r, expr.substring("action:".length()).trim());
                    }
                }
                // eventos on* (con la nota serial:, cuyos segmentos son todos referencias)
                for (String tag : TAGS_CON_EVENTOS) {
                    for (Element e : ViewFiles.byTag(doc, tag)) {
                        for (String evento : EVENTOS) {
                            String valor = ViewFiles.attr(e, evento);
                            if (valor.isBlank()) {
                                continue;
                            }
                            if (valor.startsWith("serial:")) {
                                for (String parte : valor.substring("serial:".length()).split(",")) {
                                    anhadirNoVacio(r, parte.trim());
                                }
                            } else {
                                r.add(valor);
                            }
                        }
                    }
                }
                // <action name="…"> dentro de action-group
                for (Element ag : ViewFiles.byTag(doc, "action-group")) {
                    for (Element a : ViewFiles.childrenByTag(ag, "action")) {
                        anhadirNoVacio(r, ViewFiles.attr(a, "name"));
                    }
                }
                // <dataset type="rpc"> de chart
                for (Element ch : ViewFiles.byTag(doc, "chart")) {
                    for (Element ds : ViewFiles.childrenByTag(ch, "dataset")) {
                        if ("rpc".equals(ViewFiles.attr(ds, "type")) && ds.getTextContent() != null) {
                            anhadirNoVacio(r, ds.getTextContent().trim());
                        }
                    }
                }
            }
            referenciadosCache = r;
        }
        return referenciadosCache;
    }

    private static void anhadirNoVacio(Set<String> destino, String valor) {
        if (valor != null && !valor.isBlank()) {
            destino.add(valor);
        }
    }

    /** Contenido concatenado de todos los .java bajo com/educaflow (exención de VAR-4.2). Cacheado. */
    private static synchronized String fuentesJava() {
        if (fuentesJavaCache == null) {
            StringBuilder sb = new StringBuilder();
            try (Stream<Path> walk = Files.walk(baseEducaflow())) {
                for (Path p : walk.filter(Files::isRegularFile)
                        .filter(x -> x.getFileName().toString().endsWith(".java"))
                        .sorted().toList()) {
                    sb.append(Files.readString(p)).append('\n');
                }
            } catch (IOException e) {
                throw new IllegalStateException("No se pudieron leer los fuentes Java bajo com/educaflow", e);
            }
            fuentesJavaCache = sb.toString();
        }
        return fuentesJavaCache;
    }

    // ------------------------------------------------------------------
    // VAR-4.1
    // ------------------------------------------------------------------

    /**
     * VAR-4.1 — Toda referencia de la tabla resuelve a un elemento declarado del tipo esperado:
     * {@code grid-view}/{@code form-view} → grid/form; {@code <view type="grid|form">} de un
     * action-view → grid/form; {@code action} de un menuitem → action-view; eventos on* →
     * action-group (con la nota {@code serial:}); {@code <action name>} de un action-group →
     * acción declarada o global/predefinida; {@code <dataset type="rpc">} → action-method.
     */
    // [VAR-4.1] Verificación:
    //   Cross-XML global (recorre `menus.xml` + todos los `views/`).
    //   Sujeto: cada valor de los atributos/nodos de referencia de la tabla.
    //   Condición: el valor resuelve a un elemento declarado (en cualquier fichero del ámbito) **del tipo esperado**, o a una acción global/predefinida donde la tabla lo admite —
    //
    //   | Referencia                                        | Debe resolver a |
    //   |---------------------------------------------------|-----------------|
    //   | `grid-view` (de `field` o `panel-related`)         | `<grid>` |
    //   | `form-view` (de `field` o `panel-related`)         | `<form>` |
    //   | `<view type="grid" name="…">` de un `action-view`  | `<grid>` |
    //   | `<view type="form" name="…">` de un `action-view`  | `<form>` |
    //   | `action` de un `<menuitem>`                        | `<action-view>` |
    //   | `action` de un `<panel-dashlet>`                   | `<action-view>` |
    //   | eventos `on*`/`onClick` de `form`/`field`/`button` | `<action-group>` (ver nota `serial:`) |
    //   | `<action name="…">` dentro de un `action-group`    | cualquier acción declarada o global/predefinida |
    //   | `<dataset type="rpc">…</dataset>` de un `<chart>`  | `<action-method>` |
    //   | `expr="action:{name}"` de un `<field>`             | acción declarada (normalmente `<action-method>`) |
    //
    //   Nota `serial:`: en un evento con prefijo `serial:` (AutoFirma), el **último** segmento de la lista debe resolver a un `<action-group>`; los segmentos anteriores, a acciones declaradas.
    @Test
    void var4_1_todaReferenciaResuelveAlTipoEsperado() {
        Declarados d = declarados();
        List<Violacion> v = new ArrayList<>();

        for (ViewFile vf : ViewFiles.all()) {
            String fich = vf.rel();

            // grid-view / form-view de field y panel-related
            for (String tag : List.of("field", "panel-related")) {
                for (Element e : vf.byTag(tag)) {
                    String ub = "<" + tag + " name=\"" + ViewFiles.attr(e, "name") + "\">";
                    comprobar(fich, ub, "grid-view", ViewFiles.attr(e, "grid-view"), d.grids(), "<grid>", v);
                    comprobar(fich, ub, "form-view", ViewFiles.attr(e, "form-view"), d.forms(), "<form>", v);
                }
            }

            // <view type="grid|form" name="…"> de action-view
            for (Element av : vf.actionViews()) {
                String ub = "<action-view name=\"" + ViewFiles.attr(av, "name") + "\">";
                for (Element view : ViewFiles.childrenByTag(av, "view")) {
                    String type = ViewFiles.attr(view, "type");
                    if ("grid".equals(type)) {
                        comprobar(fich, ub, "view type=\"grid\" name", ViewFiles.attr(view, "name"),
                                d.grids(), "<grid>", v);
                    } else if ("form".equals(type)) {
                        comprobar(fich, ub, "view type=\"form\" name", ViewFiles.attr(view, "name"),
                                d.forms(), "<form>", v);
                    }
                }
            }

            // eventos on* de form/field/button/panel/panel-related
            for (String tag : TAGS_CON_EVENTOS) {
                for (Element e : vf.byTag(tag)) {
                    String ub = "<" + tag + " name=\"" + ViewFiles.attr(e, "name") + "\">";
                    for (String evento : EVENTOS) {
                        String valor = ViewFiles.attr(e, evento);
                        if (!valor.isBlank()) {
                            comprobarEvento(fich, ub, evento, valor, d, v);
                        }
                    }
                }
            }

            // <action name="…"> dentro de action-group
            for (Element ag : vf.byTag("action-group")) {
                String ub = "<action-group name=\"" + ViewFiles.attr(ag, "name") + "\">";
                for (Element a : ViewFiles.childrenByTag(ag, "action")) {
                    String name = ViewFiles.attr(a, "name");
                    if (name.isBlank()) {
                        continue;
                    }
                    if (!d.acciones().contains(name) && !d.actionViews().contains(name)
                            && !Index.PREDEFINIDAS.contains(name)) {
                        v.add(new Violacion(fich, ub, "<action name=\"" + name
                                + "\"> no resuelve a ninguna acción declarada ni global/predefinida"));
                    }
                }
            }

            // action de <panel-dashlet>
            for (Element pd : vf.byTag("panel-dashlet")) {
                String ub = "<panel-dashlet name=\"" + ViewFiles.attr(pd, "name") + "\">";
                comprobar(fich, ub, "action", ViewFiles.attr(pd, "action"), d.actionViews(), "<action-view>", v);
            }

            // expr="action:{name}" de <field>
            for (Element fl : vf.byTag("field")) {
                String expr = ViewFiles.attr(fl, "expr");
                if (expr.startsWith("action:")) {
                    String ref = expr.substring("action:".length()).trim();
                    String ub = "<field name=\"" + ViewFiles.attr(fl, "name") + "\">";
                    if (!d.acciones().contains(ref) && !d.actionViews().contains(ref)) {
                        v.add(new Violacion(fich, ub, "expr=\"" + expr
                                + "\" no resuelve a ninguna acción declarada"));
                    }
                }
            }

            // <dataset type="rpc"> de chart
            for (Element ch : vf.byTag("chart")) {
                String ub = "<chart name=\"" + ViewFiles.attr(ch, "name") + "\">";
                for (Element ds : ViewFiles.childrenByTag(ch, "dataset")) {
                    if ("rpc".equals(ViewFiles.attr(ds, "type"))) {
                        String ref = ds.getTextContent() == null ? "" : ds.getTextContent().trim();
                        comprobar(fich, ub, "dataset rpc", ref, d.actionMethods(), "<action-method>", v);
                    }
                }
            }
        }

        // action de <menuitem> en menus.xml
        for (Element mi : ViewFiles.byTag(ViewFiles.menusDoc(), "menuitem")) {
            String accion = ViewFiles.attr(mi, "action");
            if (!accion.isBlank() && !d.actionViews().contains(accion)) {
                v.add(new Violacion("secretariavirtual/menus/menus.xml",
                        "<menuitem name=\"" + ViewFiles.attr(mi, "name") + "\">",
                        "action=\"" + accion + "\" no resuelve a ningún <action-view>"));
            }
        }

        Violacion.assertNone("VAR-4.1 — Toda referencia resuelve a un elemento existente del tipo esperado", v);
    }

    /** Comprueba una referencia simple: si tiene valor, debe estar en el conjunto declarado. */
    private static void comprobar(String fichero, String ubicacion, String atributo, String valor,
                                  Set<String> declarados, String tipoEsperado, List<Violacion> v) {
        if (!valor.isBlank() && !declarados.contains(valor)) {
            v.add(new Violacion(fichero, ubicacion, atributo + "=\"" + valor
                    + "\" no resuelve a ningún " + tipoEsperado + " declarado"));
        }
    }

    /**
     * Comprueba el valor de un evento: debe resolver a un {@code <action-group>}. Nota
     * {@code serial:}: el último segmento de la lista debe resolver a un action-group y los
     * anteriores a acciones declaradas.
     */
    private static void comprobarEvento(String fichero, String ubicacion, String evento, String valor,
                                        Declarados d, List<Violacion> v) {
        if (valor.startsWith("serial:")) {
            List<String> partes = Arrays.stream(valor.substring("serial:".length()).split(","))
                    .map(String::trim).toList();
            for (int i = 0; i < partes.size(); i++) {
                String parte = partes.get(i);
                if (i == partes.size() - 1) {
                    if (!d.actionGroups().contains(parte)) {
                        v.add(new Violacion(fichero, ubicacion, evento + "=\"" + valor
                                + "\": el último segmento de la lista serial: (" + parte
                                + ") no resuelve a ningún <action-group>"));
                    }
                } else if (!d.acciones().contains(parte) && !d.actionViews().contains(parte)) {
                    v.add(new Violacion(fichero, ubicacion, evento + "=\"" + valor
                            + "\": el segmento " + parte
                            + " de la lista serial: no resuelve a ninguna acción declarada"));
                }
            }
        } else if (!d.actionGroups().contains(valor)) {
            v.add(new Violacion(fichero, ubicacion, evento + "=\"" + valor
                    + "\" no resuelve a ningún <action-group>"));
        }
    }

    // ------------------------------------------------------------------
    // VAR-4.2
    // ------------------------------------------------------------------

    /**
     * VAR-4.2 — Toda acción declarada con '@' (action-view/group/method/script/record/attrs/
     * validate/condition) aparece referenciada en al menos uno de los sitios de la tabla de
     * VAR-4.1. Exención: los action-view que abre el servidor por código (openView), detectados
     * buscando el name literal en los .java bajo com/educaflow.
     */
    // [VAR-4.2] Verificación:
    //   Cross-XML global (recorre `menus.xml` + todos los `views/`).
    //   Sujeto: cada acción declarada con `name` que contiene `@` — `action-view`, `action-group`, `action-method`, `action-script`, `action-record`, `action-attrs`, `action-validate`, `action-condition`.
    //   Condición: su `name` aparece referenciado en **al menos uno** de los sitios de la tabla de `VAR-4.1`.
    //   Exención: acciones globales/predefinidas (que no se declaran en los ficheros) y los `action-view` de utilidad que abre el servidor por código (controlador vía `openView`), no visibles en el XML.
    @Test
    void var4_2_todaAccionDeclaradaEstaReferenciada() {
        Set<String> refs = referenciados();
        List<Violacion> v = new ArrayList<>();
        for (ViewFile vf : ViewFiles.all()) {
            for (String tag : TAGS_ACCION) {
                for (Element e : vf.byTag(tag)) {
                    String name = ViewFiles.attr(e, "name");
                    if (!name.contains("@") || refs.contains(name)) {
                        continue;
                    }
                    if ("action-view".equals(tag) && fuentesJava().contains(name)) {
                        continue; // action-view de utilidad que abre el servidor por código
                    }
                    v.add(new Violacion(vf.rel(), "<" + tag + " name=\"" + name + "\">",
                            "acción huérfana: no la referencia ningún sitio de la tabla de VAR-4.1"));
                }
            }
        }
        Violacion.assertNone("VAR-4.2 — Toda acción declarada está referenciada (sin acciones huérfanas)", v);
    }
}
