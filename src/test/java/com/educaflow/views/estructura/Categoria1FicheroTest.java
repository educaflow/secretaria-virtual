// =====================================================================
// GENERADO por /code-create-view-tests desde agent_docs/view-rules.md
// NO EDITAR A MANO. Para cambiar un test, edita view-rules.md (o corrige
// la traducción en el skill /code-create-view-tests) y vuelve a ejecutarlo.
// =====================================================================
package com.educaflow.views.estructura;

import com.educaflow.views.support.ViewFile;
import com.educaflow.views.support.ViewFiles;
import com.educaflow.views.support.Violacion;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * Categoría 1 — Fichero y ubicación (agent_docs/view-rules.md).
 *
 * <p>Tests JUnit 5 planos (sin ArchUnit) que verifican las reglas VAR-1.1 a VAR-1.3 leyendo los
 * XML de vistas con JAXP/DOM a través de {@link ViewFiles}. Cada test implementa exactamente el
 * bloque «Verificación» de su regla (Sujeto + Condición + Exenciones) y acumula TODAS las
 * violaciones antes de fallar.
 */
class Categoria1FicheroTest {

    private static final String NS_OBJECT_VIEWS = "http://axelor.com/xml/ns/object-views";
    private static final String XSD_8_1 = "object-views_8.1.xsd";

    /** VAR-1.2: dos palabras en UpperCamelCase separadas por un guion, sin más caracteres. */
    private static final Pattern PATRON_FICHERO =
            Pattern.compile("^[A-Z][A-Za-z0-9]*-[A-Z][A-Za-z0-9]*\\.xml$");

    // [VAR-1.1] Verificación:
    //   Sujeto: cada fichero `views/*.xml` no exento.
    //   Condición: el elemento raíz es `object-views`, con `xmlns="http://axelor.com/xml/ns/object-views"` y `xsi:schemaLocation` a `object-views_8.1.xsd`.
    @Test
    void var1_1_cabeceraFicheroVistas() {
        List<Violacion> v = new ArrayList<>();
        // Sujeto: cada fichero views/*.xml no exento.
        for (ViewFile vf : ViewFiles.all()) {
            Element raiz = vf.doc().getDocumentElement();
            if (!"object-views".equals(raiz.getNodeName())) {
                v.add(new Violacion(vf.rel(), "<" + raiz.getNodeName() + ">",
                        "el elemento raíz debe ser <object-views>"));
                continue;
            }
            // El parser no es namespace-aware: xmlns y xsi:schemaLocation son atributos literales.
            String xmlns = ViewFiles.attr(raiz, "xmlns");
            if (!NS_OBJECT_VIEWS.equals(xmlns)) {
                v.add(new Violacion(vf.rel(), "object-views",
                        "xmlns debe ser \"" + NS_OBJECT_VIEWS + "\" (actual: \"" + xmlns + "\")"));
            }
            String schemaLocation = ViewFiles.attr(raiz, "xsi:schemaLocation");
            if (!schemaLocation.contains(XSD_8_1)) {
                v.add(new Violacion(vf.rel(), "object-views",
                        "xsi:schemaLocation debe apuntar a " + XSD_8_1
                                + " (actual: \"" + schemaLocation + "\")"));
            }
        }
        Violacion.assertNone("VAR-1.1 — Todo fichero de vistas tiene raíz <object-views> con xmlns \""
                + NS_OBJECT_VIEWS + "\" y xsi:schemaLocation a " + XSD_8_1, v);
    }

    // [VAR-1.2] Verificación:
    //   Sujeto: cada fichero `views/*.xml` no exento.
    //   Condición:
    //     (a) el nombre del fichero es `{Variante}-{Entidad}.xml` — dos palabras en formato upper camel case separadas por un guion, sin espacios ni otros caracteres;
    //     (b) la `{Entidad}` existe como entidad en el XML del modelo del módulo (`../domains`).
    //   Exenciones:
    //     los ficheros de **overrides de vistas del framework Axelor** — aquellos cuyos elementos de alto nivel llevan todos un `name` sin `@` (p.ej. `user-preferences-form-view.xml`) — no forman bloques (`VAR-3.1`) y no siguen el patrón;
    //     y los ficheros cuyas vistas apuntan todas a **modelos del framework** (ningún `model` empieza por `com.educaflow`) quedan exentos de (b): su `{Entidad}` es el nombre en español de la entidad del framework (`Usuario`→`User`, `Anexos`→`MetaFile`), coherente con la exención de `VAR-2.3`.
    @Test
    void var1_2_nombreFicheroEsVarianteGuionEntidad() {
        List<Violacion> v = new ArrayList<>();
        // Sujeto: cada fichero views/*.xml no exento.
        for (ViewFile vf : ViewFiles.all()) {
            // Exención: overrides de vistas del framework Axelor — TODOS los elementos de alto
            // nivel (hijos directos de object-views) llevan un name sin '@'.
            if (esOverrideAxelor(vf)) {
                continue;
            }
            String nombre = vf.fileName();
            // (a) patrón {Variante}-{Entidad}.xml
            if (!PATRON_FICHERO.matcher(nombre).matches()) {
                v.add(new Violacion(vf.rel(), nombre,
                        "el nombre del fichero no sigue el patrón {Variante}-{Entidad}.xml "
                                + "(dos palabras UpperCamelCase separadas por un guion)"));
                continue;
            }
            // Exención de (b): ficheros cuyas vistas apuntan todas a modelos del framework
            // (ningún model empieza por com.educaflow) — su Entidad es el nombre en español
            // de la entidad del framework (Usuario→User, Anexos→MetaFile), como en VAR-2.3.
            if (soloModelosDelFramework(vf)) {
                continue;
            }
            // (b) la Entidad existe en el modelo del módulo (../domains)
            String entidad = nombre.substring(nombre.indexOf('-') + 1, nombre.length() - ".xml".length());
            Set<String> entidades = ViewFiles.entidadesDelModulo(vf);
            if (!entidades.contains(entidad)) {
                v.add(new Violacion(vf.rel(), nombre,
                        "la entidad \"" + entidad + "\" no existe en el XML del modelo del módulo "
                                + "(../domains); entidades declaradas: " + new TreeSet<>(entidades)));
            }
        }
        Violacion.assertNone("VAR-1.2 — El nombre de todo fichero de vistas es {Variante}-{Entidad}.xml "
                + "y la Entidad existe en ../domains (exentos los overrides Axelor sin '@')", v);
    }

    // [VAR-1.3] Verificación:
    //   Sujeto: cada fichero del ámbito de análisis.
    //   Condición: ningún fichero de `views/` contiene un `<menuitem>`;
    //     el único fichero con `<menuitem>` es `secretariavirtual/menus/menus.xml`.
    @Test
    void var1_3_menuitemsSoloEnMenusXml() {
        List<Violacion> v = new ArrayList<>();
        // El único fichero del ámbito autorizado a contener <menuitem> es ViewFiles.menusPath()
        // (secretariavirtual/menus/menus.xml), que NO está bajo views/ y por tanto no forma parte
        // de ViewFiles.all(): basta con exigir que ningún fichero de views/ contenga <menuitem>.
        for (ViewFile vf : ViewFiles.all()) {
            for (Element mi : vf.byTag("menuitem")) {
                v.add(new Violacion(vf.rel(), "<menuitem name=\"" + ViewFiles.attr(mi, "name") + "\">",
                        "los <menuitem> no pueden vivir en un fichero de views/: todo el árbol de "
                                + "menús se declara en " + ViewFiles.menusPath()));
            }
        }
        Violacion.assertNone("VAR-1.3 — Ningún fichero de views/ contiene <menuitem>; "
                + "todos los menús viven solo en secretariavirtual/menus/menus.xml", v);
    }

    // ---- helpers privados ----

    /**
     * Exención de VAR-1.2: un fichero es un override de vistas del framework Axelor cuando todos
     * sus elementos de alto nivel (hijos directos de object-views) llevan un name sin '@'
     * (p.ej. user-preferences-form-view.xml).
     */
    private static boolean esOverrideAxelor(ViewFile vf) {
        for (Element hijo : hijosDirectos(vf.doc().getDocumentElement())) {
            if (ViewFiles.attr(hijo, "name").contains("@")) {
                return false;
            }
        }
        return true;
    }

    /**
     * Exención de VAR-1.2(b): el fichero solo contiene vistas sobre modelos del framework Axelor
     * (tiene algún {@code model} y ninguno empieza por {@code com.educaflow}).
     */
    private static boolean soloModelosDelFramework(ViewFile vf) {
        boolean hayModel = false;
        for (Element e : ViewFiles.byTag(vf.doc(), "*")) {
            String model = ViewFiles.attr(e, "model");
            if (!model.isBlank()) {
                hayModel = true;
                if (model.startsWith("com.educaflow")) {
                    return false;
                }
            }
        }
        return hayModel;
    }

    /** Todos los hijos directos de tipo elemento (cualquier tag). */
    private static List<Element> hijosDirectos(Element padre) {
        List<Element> out = new ArrayList<>();
        NodeList nl = padre.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node n = nl.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                out.add((Element) n);
            }
        }
        return out;
    }
}
