// =====================================================================
// GENERADO por /code-create-view-tests desde agent_docs/view-rules.md
// NO EDITAR A MANO. Para cambiar un test, edita view-rules.md (o corrige
// la traducción en el skill /code-create-view-tests) y vuelve a ejecutarlo.
// =====================================================================
package com.educaflow.views.nombres;

import com.educaflow.views.support.NombreVista;
import com.educaflow.views.support.ViewFile;
import com.educaflow.views.support.ViewFiles;
import com.educaflow.views.support.Violacion;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Categoría 2 — Nomenclatura (agent_docs/view-rules.md).
 *
 * <p>Tests JUnit 5 planos (sin ArchUnit) que verifican las reglas VAR-2.1 a VAR-2.6 leyendo los
 * XML de vistas con JAXP/DOM a través de {@link ViewFiles}. Cada test implementa exactamente el
 * bloque «Verificación» de su regla (Sujeto + Condición + Exenciones) y acumula TODAS las
 * violaciones antes de fallar.
 */
class Categoria2NomenclaturaTest {

    /** Elementos de alto nivel (glosario «Elemento de alto nivel vs acción»). */
    private static final Set<String> ALTO_NIVEL = Set.of("action-view", "grid", "form", "tree", "chart");

    /** Acciones (glosario «Elemento de alto nivel vs acción»). */
    private static final Set<String> ACCIONES = Set.of(
            "action-group", "action-record", "action-attrs", "action-method",
            "action-script", "action-validate", "action-condition");

    /** Patrón de fichero {Variante}-{Entidad}.xml (VAR-1.2), con las dos palabras capturadas. */
    private static final Pattern PATRON_FICHERO =
            Pattern.compile("^([A-Z][A-Za-z0-9]*)-([A-Z][A-Za-z0-9]*)\\.xml$");

    /** Ruta relativa legible de menus.xml, para los mensajes de VAR-2.6. */
    private static final String MENUS_REL = "secretariavirtual/menus/menus.xml";

    // -------------------------------------------------------------------------------------------
    // VAR-2.1 — El contexto de cada name se deriva de la ubicación
    // -------------------------------------------------------------------------------------------

    // [VAR-2.1] Verificación:
    //   Sujeto: el `name` de cada hijo directo de `<object-views>` de un fichero `{Variante}-{Entidad}.xml` bajo `system/<x>/views/` o `subsystem/<x>/views/`.
    //   Condición: el `name` empieza por `{marcadorCapa}{Módulo}.{Variante}@{Entidad}`, donde —
    //     `marcadorCapa` = `subsys` si el fichero está bajo `subsystem/`, `sys` si está bajo `system/`;
    //     `Módulo` = el nombre de la carpeta `<x>` (comparación case-insensitive);
    //     `Variante` y `Entidad` = las dos palabras del nombre del fichero;
    //     y tras ese prefijo sigue `-` (elemento del bloque maestro) o `.` (ruta de detalle `.{EntidadDetalle}[…]` y después `-`).
    //   Exenciones: acciones globales/predefinidas sin `@` (`VAR-2.5`) y nombres Axelor adaptados (p.ej. `user-preferences-form`).
    //     (Los nombres `exp-` del framework de expedientes no necesitan exención: viven en paquetes exentos, fuera del sujeto.)
    @Test
    void var2_1_contextoDerivadoDeUbicacion() {
        List<Violacion> v = new ArrayList<>();
        for (ViewFile vf : ViewFiles.all()) {
            // Sujeto: ficheros {Variante}-{Entidad}.xml bajo system/<x>/views/ o subsystem/<x>/views/.
            Matcher m = PATRON_FICHERO.matcher(vf.fileName());
            if (!m.matches() || !(vf.isSystem() || vf.isSubsystem())) {
                continue;
            }
            String variante = m.group(1);
            String entidad = m.group(2);
            String marcadorCapa = vf.isSubsystem() ? "subsys" : "sys";
            for (Element hijo : hijosDirectos(vf.doc().getDocumentElement())) {
                String name = ViewFiles.attr(hijo, "name");
                // Exenciones: acciones globales/predefinidas y nombres Axelor adaptados (sin '@').
                if (!name.contains("@")) {
                    continue;
                }
                String problema = compruebaPrefijoContexto(name, marcadorCapa, vf.ownerModule(), variante, entidad);
                if (problema != null) {
                    v.add(new Violacion(vf.rel(), name, problema));
                }
            }
        }
        Violacion.assertNone("VAR-2.1 — El name de todo hijo directo de <object-views> empieza por "
                + "{marcadorCapa}{Módulo}.{Variante}@{Entidad} derivado de la carpeta y el nombre del "
                + "fichero, seguido de '-' o '.'", v);
    }

    /**
     * Comprueba que el name empieza por {marcadorCapa}{Módulo}.{Variante}@{Entidad} (Módulo
     * comparado case-insensitive con la carpeta) seguido de '-' (bloque maestro) o '.' (detalle).
     * Devuelve el detalle de la violación, o null si cumple.
     */
    private static String compruebaPrefijoContexto(String name, String marcadorCapa,
                                                   String moduloCarpeta, String variante, String entidad) {
        if (!name.startsWith(marcadorCapa)) {
            return "el name debe empezar por el marcador de capa \"" + marcadorCapa + "\"";
        }
        String resto = name.substring(marcadorCapa.length());
        int punto = resto.indexOf('.');
        if (punto <= 0) {
            return "tras el marcador \"" + marcadorCapa + "\" debe venir {Módulo}.{Variante}@…";
        }
        String modulo = resto.substring(0, punto);
        if (!modulo.equalsIgnoreCase(moduloCarpeta)) {
            return "el Módulo \"" + modulo + "\" no casa con la carpeta \"" + moduloCarpeta + "\"";
        }
        String trasModulo = resto.substring(punto + 1);
        String esperado = variante + "@" + entidad;
        if (!trasModulo.startsWith(esperado)) {
            return "tras el Módulo debe venir \"" + esperado + "\" ({Variante}@{Entidad} del nombre del fichero)";
        }
        String cola = trasModulo.substring(esperado.length());
        if (cola.isEmpty() || (cola.charAt(0) != '-' && cola.charAt(0) != '.')) {
            return "tras el prefijo \"" + marcadorCapa + modulo + "." + esperado
                    + "\" debe seguir '-' (elemento del bloque maestro) o '.' (ruta de detalle)";
        }
        return null;
    }

    // -------------------------------------------------------------------------------------------
    // VAR-2.2 — El name dice el tipo y el rol del elemento
    // -------------------------------------------------------------------------------------------

    // [VAR-2.2] Verificación:
    //   Sujeto: cada elemento de alto nivel o acción con `name` que contenga `@`.
    //   Condición:
    //     (a) el último segmento (`tipo`) casa con el elemento: `grid`/`form`/`tree`/`chart` llevan su homónimo (`-grid`, `-form`, `-tree`, `-chart`); el `action-view` y toda acción llevan `-action`;
    //     (b) toda acción salvo el `action-view` es de los elementos admitidos por el **rol** que declara su `descripcion` (tabla «Rol de una acción» del glosario).
    //   Exenciones: acciones globales (`VAR-2.5`) y nombres Axelor adaptados.
    @Test
    void var2_2a_tipoDelNameCasaConElElemento() {
        List<Violacion> v = new ArrayList<>();
        for (ViewFile vf : ViewFiles.all()) {
            for (String tag : union(ALTO_NIVEL, ACCIONES)) {
                for (Element e : vf.byTag(tag)) {
                    String name = ViewFiles.attr(e, "name");
                    // Exenciones: acciones globales y nombres Axelor adaptados (sin '@').
                    if (!name.contains("@")) {
                        continue;
                    }
                    // grid/form/tree/chart llevan su homónimo; action-view y toda acción llevan "action".
                    String tipoEsperado = switch (tag) {
                        case "grid", "form", "tree", "chart" -> tag;
                        default -> "action";
                    };
                    int ultimoGuion = name.lastIndexOf('-');
                    String tipo = ultimoGuion < 0 ? "" : name.substring(ultimoGuion + 1);
                    if (!tipo.equals(tipoEsperado)) {
                        v.add(new Violacion(vf.rel(), name,
                                "el tipo del name (último segmento) es \"" + tipo + "\" pero un <" + tag
                                        + "> debe acabar en \"-" + tipoEsperado + "\""));
                    }
                }
            }
        }
        Violacion.assertNone("VAR-2.2(a) — El último segmento del name casa con el elemento: "
                + "grid/form/tree/chart llevan su homónimo; action-view y toda acción llevan -action", v);
    }

    // [VAR-2.2] (continuación)
    @Test
    void var2_2b_rolDeLaDescripcionAdmiteElElemento() {
        List<Violacion> v = new ArrayList<>();
        for (ViewFile vf : ViewFiles.all()) {
            // Sujeto: toda acción salvo el action-view (que no lleva marcador de rol).
            for (String tag : ACCIONES) {
                for (Element e : vf.byTag(tag)) {
                    String name = ViewFiles.attr(e, "name");
                    if (!name.contains("@")) {
                        continue; // acciones globales/predefinidas: exentas
                    }
                    NombreVista nv = NombreVista.parse(name);
                    if (nv == null) {
                        continue; // prefijo de contexto no parseable: lo reporta VAR-2.1
                    }
                    Set<String> admitidos = elementosAdmitidosPorRol(nv.descripcion());
                    if (admitidos == null) {
                        v.add(new Violacion(vf.rel(), name,
                                "la descripcion \"" + nv.descripcion() + "\" no empieza por ningún marcador "
                                        + "de rol (btn{X}, on{Evento}, Local-, set-…, Remote-)"));
                    } else if (!admitidos.contains(tag)) {
                        v.add(new Violacion(vf.rel(), name,
                                "el rol que declara la descripcion \"" + nv.descripcion() + "\" admite "
                                        + admitidos + ", pero el elemento es <" + tag + ">"));
                    }
                }
            }
        }
        Violacion.assertNone("VAR-2.2(b) — Toda acción (salvo el action-view) usa un elemento admitido "
                + "por el rol que declara el arranque de su descripcion (tabla «Rol de una acción»)", v);
    }

    /**
     * Tabla «Rol de una acción» del glosario: marcador con que empieza la descripcion → elementos
     * XML admitidos. Devuelve null si la descripcion no empieza por ningún marcador (violación).
     *
     * <p>Detalle de los marcadores: btn{X} se reconoce por el prefijo "btn"; on{Evento} exige
     * mayúscula tras "on" (onNew, onLoad…) para no confundirse con palabras que empiecen por "on";
     * en set-… el segmento campo (entre "set-" y el siguiente '-') decide: sin punto →
     * set-{campo}-{valor} (action-record), con punto → set-{campo}.{atributo}-{valor} (action-attrs).
     */
    private static Set<String> elementosAdmitidosPorRol(String descripcion) {
        if (descripcion.startsWith("btn") || descripcion.matches("^on[A-Z].*")) {
            return Set.of("action-group"); // principal (botón/evento)
        }
        if (descripcion.startsWith("Local-")) {
            // validación local (action-group como combinador de validaciones)
            return Set.of("action-validate", "action-condition", "action-group");
        }
        if (descripcion.startsWith("set-")) {
            String resto = descripcion.substring("set-".length());
            int guion = resto.indexOf('-');
            String campo = guion < 0 ? resto : resto.substring(0, guion);
            return campo.contains(".") ? Set.of("action-attrs") : Set.of("action-record"); // regla de campo
        }
        if (descripcion.startsWith("Remote-")) {
            return Set.of("action-method", "action-script"); // remota
        }
        return null;
    }

    // -------------------------------------------------------------------------------------------
    // VAR-2.3 — La entidad del bloque = clase del model
    // -------------------------------------------------------------------------------------------

    // [VAR-2.3] Verificación:
    //   Sujeto: cada `grid`/`form`/`tree`/`chart`/`action-view`/`action-method`/`action-script`/`action-record` con `name` (con `@`) y atributo `model`.
    //   Condición: el último segmento de la ruta de entidad del contexto (la ruta va tras la `@`, antes del primer `-`) == último segmento del `model`.
    //   Exención: elementos cuyo `model` no pertenece a `com.educaflow` (entidades del framework Axelor):
    //     su entidad del contexto es deliberadamente el nombre adaptado al español (p.ej. `Usuario`→`com.axelor.auth.db.User`, `Anexos`→`com.axelor.meta.db.MetaFile`).
    @Test
    void var2_3_entidadDelContextoIgualClaseDelModel() {
        List<Violacion> v = new ArrayList<>();
        Set<String> tags = Set.of("grid", "form", "tree", "chart",
                "action-view", "action-method", "action-script", "action-record");
        for (ViewFile vf : ViewFiles.all()) {
            for (String tag : tags) {
                for (Element e : vf.byTag(tag)) {
                    // Sujeto: elementos con name (con '@') y atributo model.
                    if (!ViewFiles.hasAttr(e, "model")) {
                        continue;
                    }
                    String name = ViewFiles.attr(e, "name");
                    if (!name.contains("@")) {
                        continue;
                    }
                    NombreVista nv = NombreVista.parse(name);
                    if (nv == null) {
                        continue; // prefijo de contexto no parseable: lo reporta VAR-2.1
                    }
                    String model = ViewFiles.attr(e, "model");
                    // Exención: entidades del framework Axelor (model fuera de com.educaflow) —
                    // su entidad del contexto es el nombre adaptado al español (Usuario→User).
                    if (!model.startsWith("com.educaflow")) {
                        continue;
                    }
                    String claseModel = model.substring(model.lastIndexOf('.') + 1);
                    if (!nv.entidad().equals(claseModel)) {
                        v.add(new Violacion(vf.rel(), name,
                                "la entidad del contexto es \"" + nv.entidad()
                                        + "\" pero el model apunta a \"" + claseModel + "\" (" + model + ")"));
                    }
                }
            }
        }
        Violacion.assertNone("VAR-2.3 — El último segmento de la ruta de entidad del contexto coincide "
                + "con el último segmento del model", v);
    }

    // -------------------------------------------------------------------------------------------
    // VAR-2.4 — Coherencia acción↔método en action-method
    // -------------------------------------------------------------------------------------------

    // [VAR-2.4] Verificación:
    //   Sujeto: cada `<action-method>` con `name` que contenga `-Remote-{X}-action` y un `<call … method="Y">`.
    //   Condición: `X == Y` (ignorando los argumentos de `Y` si los lleva, p.ej. `enviarCorreo(id,nombre)`).
    @Test
    void var2_4_remoteXCasaConElMetodoDelCall() {
        List<Violacion> v = new ArrayList<>();
        for (ViewFile vf : ViewFiles.all()) {
            for (Element am : vf.byTag("action-method")) {
                String name = ViewFiles.attr(am, "name");
                // Sujeto: name que contiene -Remote-{X}-action y tiene <call method="Y">.
                int i = name.indexOf("-Remote-");
                if (i < 0 || !name.endsWith("-action")) {
                    continue;
                }
                String x = name.substring(i + "-Remote-".length(), name.length() - "-action".length());
                for (Element call : ViewFiles.byTag(am, "call")) {
                    String y = ViewFiles.attr(call, "method");
                    int parentesis = y.indexOf('(');
                    String metodo = parentesis < 0 ? y : y.substring(0, parentesis);
                    if (!x.equals(metodo)) {
                        v.add(new Violacion(vf.rel(), name,
                                "el name declara el método \"" + x + "\" pero el <call> invoca \""
                                        + metodo + "\""));
                    }
                }
            }
        }
        Violacion.assertNone("VAR-2.4 — En todo action-method …-Remote-{X}-action, X coincide con el "
                + "method del <call> (ignorando sus argumentos)", v);
    }

    // -------------------------------------------------------------------------------------------
    // VAR-2.5 — La validación remota de save/delete es única y global
    // -------------------------------------------------------------------------------------------

    // [VAR-2.5] Verificación:
    //   Sujeto: cada `<action-method>` y cada `name`/`<action name>` de los ficheros de `views/`.
    //   Condición:
    //     (a) las cadenas `remote-validationSave-action`/`remote-validationDelete-action` aparecen **solo** como `<action name="…"/>` (referencia dentro de un `action-group`);
    //     (b) ningún `name` contiene `Remote-validationSave`, `Remote-validationDelete`, `Remote-validateSave` ni `Remote-validateDelete` (variantes por entidad de la validación global);
    //     (c) ningún `<call>` tiene `method="validationSave"` ni `method="validationDelete"`.
    @Test
    void var2_5_validacionRemotaGlobalUnica() {
        List<Violacion> v = new ArrayList<>();
        Set<String> globalesValidacion =
                Set.of("remote-validationSave-action", "remote-validationDelete-action");
        List<String> fragmentosProhibidos = List.of(
                "Remote-validationSave", "Remote-validationDelete",
                "Remote-validateSave", "Remote-validateDelete");
        Set<String> metodosProhibidos = Set.of("validationSave", "validationDelete");

        for (ViewFile vf : ViewFiles.all()) {
            NodeList todos = vf.doc().getElementsByTagName("*");
            for (int i = 0; i < todos.getLength(); i++) {
                Element e = (Element) todos.item(i);
                String tag = e.getNodeName();
                String name = ViewFiles.attr(e, "name");
                // (a) las cadenas globales solo como <action name="…"/> dentro de un action-group
                if (globalesValidacion.contains(name)) {
                    boolean esReferencia = "action".equals(tag)
                            && e.getParentNode() != null
                            && "action-group".equals(e.getParentNode().getNodeName());
                    if (!esReferencia) {
                        v.add(new Violacion(vf.rel(), name,
                                "\"" + name + "\" solo puede aparecer como <action name=\"…\"/> dentro "
                                        + "de un action-group, no declararse como <" + tag + ">"));
                    }
                }
                // (b) ningún name contiene variantes por entidad de la validación global
                for (String fragmento : fragmentosProhibidos) {
                    if (name.contains(fragmento)) {
                        v.add(new Violacion(vf.rel(), name,
                                "el name contiene \"" + fragmento + "\": no se admiten variantes por "
                                        + "entidad de la validación global de save/delete"));
                    }
                }
                // (c) ningún <call> invoca validationSave/validationDelete
                if ("call".equals(tag)) {
                    String y = ViewFiles.attr(e, "method");
                    int parentesis = y.indexOf('(');
                    String metodo = parentesis < 0 ? y : y.substring(0, parentesis);
                    if (metodosProhibidos.contains(metodo)) {
                        v.add(new Violacion(vf.rel(), "<call method=\"" + y + "\">",
                                "ningún <call> puede reexponer el método \"" + metodo
                                        + "\" de la validación global"));
                    }
                }
            }
        }
        Violacion.assertNone("VAR-2.5 — La validación remota de save/delete es única y global: solo se "
                + "referencia como <action name=\"remote-validation…-action\"/> dentro de un action-group, "
                + "sin variantes por entidad ni <call> a validationSave/validationDelete", v);
    }

    // -------------------------------------------------------------------------------------------
    // VAR-2.6 — Unicidad global de name
    // -------------------------------------------------------------------------------------------

    // [VAR-2.6] Verificación:
    //   Sujeto: el conjunto de todos los `name` de hijos directos de `<object-views>` de todos los ficheros del ámbito (incluido `menus.xml`).
    //   Condición: no hay dos elementos con el mismo `name`.
    @Test
    void var2_6_unicidadGlobalDeName() {
        // Sujeto: todos los name de hijos directos de <object-views> de todos los ficheros del
        // ámbito, incluido menus.xml.
        Map<String, List<String>> declaraciones = new LinkedHashMap<>();
        for (ViewFile vf : ViewFiles.all()) {
            registraNames(declaraciones, vf.doc().getDocumentElement(), vf.rel());
        }
        registraNames(declaraciones, ViewFiles.menusDoc().getDocumentElement(), MENUS_REL);

        List<Violacion> v = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : declaraciones.entrySet()) {
            if (entry.getValue().size() > 1) {
                v.add(new Violacion(String.join(", ", entry.getValue()), entry.getKey(),
                        "name declarado " + entry.getValue().size() + " veces; debe ser único en todo el proyecto"));
            }
        }
        Violacion.assertNone("VAR-2.6 — No hay dos hijos directos de <object-views> con el mismo name "
                + "en todo el ámbito (views/ + menus.xml)", v);
    }

    /** Acumula name → ubicaciones de los hijos directos (con name) de la raíz object-views. */
    private static void registraNames(Map<String, List<String>> declaraciones, Element raiz, String fichero) {
        for (Element hijo : hijosDirectos(raiz)) {
            String name = ViewFiles.attr(hijo, "name");
            if (!name.isBlank()) {
                declaraciones.computeIfAbsent(name, k -> new ArrayList<>())
                        .add(fichero + " <" + hijo.getNodeName() + ">");
            }
        }
    }

    // ---- helpers privados ----

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

    /** Unión inmutable de dos conjuntos de tags. */
    private static Set<String> union(Set<String> a, Set<String> b) {
        Set<String> s = new java.util.HashSet<>(a);
        s.addAll(b);
        return s;
    }
}
