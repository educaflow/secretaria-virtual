// =====================================================================
// GENERADO por /developer-create-view-tests desde agent_docs/view-rules.md
// NO EDITAR A MANO. Para cambiar un test, edita view-rules.md (o corrige
// la traducción en el skill /developer-create-view-tests) y vuelve a ejecutarlo.
// =====================================================================
package com.educaflow.views.menus;

import com.educaflow.views.support.ViewFiles;
import com.educaflow.views.support.Violacion;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.educaflow.views.support.ViewFiles.attr;
import static com.educaflow.views.support.ViewFiles.hasAttr;

/**
 * Categoría 10 — Menús (agent_docs/view-rules.md, VAR-10.1 a VAR-10.5).
 *
 * <p>Sujeto de toda la categoría: los dos ficheros de menús de {@code secretariavirtual/menus/} —
 * {@code menus.xml} (el árbol de menús de la aplicación) y {@code hide-menus.xml} (las ocultaciones
 * de menús de Axelor). Se acceden vía {@link ViewFiles#menusDoc()} / {@link ViewFiles#hideMenusDoc()}
 * para el DOM y {@link ViewFiles#menusLineas()} / {@link ViewFiles#hideMenusLineas()} para las
 * reglas de formato sobre el texto crudo.
 */
class Categoria10MenusTest {

    /** Etiqueta de fichero para las violaciones de esta categoría. */
    private static final String MENUS = "secretariavirtual/menus/menus.xml";

    /** Etiqueta del fichero de ocultaciones. */
    private static final String HIDE_MENUS = "secretariavirtual/menus/hide-menus.xml";

    /** Prefijo obligatorio del id de los menuitem de hide-menus.xml (VAR-10.5). */
    private static final String PREFIJO_ID_OCULTACION = "secretariaVirtual-";

    /** Valores canónicos admitidos para el atributo groups (VAR-10.1). */
    private static final Set<String> GROUPS_CANONICOS = Set.of("admins", "admins,users", "users");

    /** Orden relativo canónico de los atributos de un menuitem (VAR-10.3.b). */
    private static final List<String> ORDEN_ATRIBUTOS =
            List.of("name", "parent", "title", "action", "icon", "groups", "if", "order");

    /**
     * VAR-10.1 — Atributos obligatorios name/title/order/groups, con groups canónico.
     * (a) cada menuitem tiene name, title, order y groups;
     * (b) groups vale EXACTAMENTE "admins", "admins,users" o "users" (nada más;
     * la variante desordenada "users,admins" también es violación).
     */
    // [VAR-10.1] Verificación:
    //   Sujeto: cada `<menuitem>`.
    //   Condición:
    //     (a) tiene los atributos `name`, `title`, `order` y `groups`;
    //     (b) el valor de `groups` es **exactamente** uno de `admins`, `admins,users` o `users` — no se admite ningún otro valor ni la variante desordenada `users,admins`.
    //   Exenciones: los `<menuitem>` de `hide-menus.xml`, que no tienen ni `order` ni `groups` (ver la exención de categoría y `VAR-10.5`).
    @Test
    void var10_1_atributosObligatoriosYGroupsCanonico() {
        List<Violacion> v = new ArrayList<>();
        // Exención: hide-menus.xml queda fuera del sujeto de esta regla.
        for (Element mi : ViewFiles.byTag(ViewFiles.menusDoc(), "menuitem")) {
            String ubicacion = hasAttr(mi, "name") ? attr(mi, "name") : "<menuitem sin name>";
            for (String obligatorio : List.of("name", "title", "order", "groups")) {
                if (!hasAttr(mi, obligatorio)) {
                    v.add(new Violacion(MENUS, ubicacion, "falta el atributo obligatorio " + obligatorio));
                }
            }
            if (hasAttr(mi, "groups") && !GROUPS_CANONICOS.contains(attr(mi, "groups"))) {
                v.add(new Violacion(MENUS, ubicacion,
                        "groups=\"" + attr(mi, "groups") + "\" no es canónico"
                                + " (solo se admite \"admins\", \"admins,users\" o \"users\")"));
            }
        }
        Violacion.assertNone(
                "VAR-10.1 — Todo <menuitem> lleva name/title/order/groups y groups es canónico",
                v);
    }

    /**
     * VAR-10.2 — {@code order} entero único por submenú: los menuitem con el mismo parent
     * (los sin parent forman su propio grupo raíz) tienen order enteros y distintos entre sí.
     */
    // [VAR-10.2] Verificación:
    //   Sujeto: los `<menuitem>` con el mismo `parent`.
    //   Condición: sus `order` son enteros distintos.
    //   Exenciones: los `<menuitem>` de `hide-menus.xml`, que no llevan `order` (ver la exención de categoría y `VAR-10.5`).
    @Test
    void var10_2_orderEnteroUnicoPorSubmenu() {
        List<Violacion> v = new ArrayList<>();
        // parent -> (order -> name del primer menuitem visto con ese order)
        Map<String, Map<Integer, String>> porPadre = new LinkedHashMap<>();
        // Exención: hide-menus.xml queda fuera del sujeto de esta regla.
        for (Element mi : ViewFiles.byTag(ViewFiles.menusDoc(), "menuitem")) {
            String nombre = hasAttr(mi, "name") ? attr(mi, "name") : "<menuitem sin name>";
            String parent = attr(mi, "parent"); // "" = grupo raíz
            String order = attr(mi, "order");
            int valor;
            try {
                valor = Integer.parseInt(order.trim());
            } catch (NumberFormatException e) {
                v.add(new Violacion(MENUS, nombre,
                        "order=\"" + order + "\" no es un entero"));
                continue;
            }
            String repetido = porPadre.computeIfAbsent(parent, k -> new LinkedHashMap<>())
                    .putIfAbsent(valor, nombre);
            if (repetido != null) {
                v.add(new Violacion(MENUS, nombre,
                        "order=\"" + valor + "\" repetido dentro del submenú de parent=\""
                                + (parent.isEmpty() ? "(raíz)" : parent) + "\" (ya lo usa " + repetido + ")"));
            }
        }
        Violacion.assertNone(
                "VAR-10.2 — Los order de los <menuitem> de un mismo parent son enteros distintos",
                v);
    }

    /**
     * VAR-10.3 — Formato del texto crudo:
     * (a) un menuitem por línea completa (ni dos en la misma línea ni uno partido);
     * (b) atributos en el orden relativo name, parent, title, action, icon, groups, if, order,
     * separados por UN solo espacio (sin dobles espacios de alineación dentro del tag);
     * (c) sangría de la línea = 4 × profundidad del menuitem en el árbol name/parent.
     */
    // [VAR-10.3] Verificación:
    //   Sujeto: cada `<menuitem>` de los dos ficheros de menús (y el texto de cada fichero).
    //   Condición:
    //     (a) no hay dos `<menuitem>` en la misma línea ni un `<menuitem>` partido en varias líneas;
    //     (b) los atributos presentes respetan el orden relativo `name, parent, title, action, icon, groups, if, order` y se separan con un único espacio (sin espacios extra de alineación);
    //     (c) sangría = 4 × (profundidad de `parent`).
    @Test
    void var10_3_formatoUnaLineaOrdenAtributosYSangria() {
        List<Violacion> v = new ArrayList<>();
        formato(MENUS, ViewFiles.menusDoc(), ViewFiles.menusLineas(), v);
        formato(HIDE_MENUS, ViewFiles.hideMenusDoc(), ViewFiles.hideMenusLineas(), v);
        Violacion.assertNone(
                "VAR-10.3 — Un <menuitem> por línea, atributos en orden fijo con un solo espacio y sangría 4×profundidad",
                v);
    }

    /** Comprueba (a), (b) y (c) de VAR-10.3 sobre el texto crudo de un fichero de menús. */
    private static void formato(String fichero, org.w3c.dom.Document doc, List<String> lineasCrudas,
                                List<Violacion> v) {
        Map<String, String> padrePorNombre = padrePorNombre(doc);
        Pattern abreMenuitem = Pattern.compile("<menuitem\\b");
        Pattern atributo = Pattern.compile("([A-Za-z_][A-Za-z0-9_-]*)=\"");
        Pattern atributoName = Pattern.compile("\\bname=\"([^\"]*)\"");

        List<String> lineas = lineasSinComentarios(lineasCrudas);
        for (int i = 0; i < lineas.size(); i++) {
            String linea = lineas.get(i);
            int numLinea = i + 1;
            Matcher m = abreMenuitem.matcher(linea);
            int apariciones = 0;
            int inicioTag = -1;
            while (m.find()) {
                if (apariciones == 0) {
                    inicioTag = m.start();
                }
                apariciones++;
            }
            if (apariciones == 0) {
                continue;
            }
            String ubicacion = "línea " + numLinea;

            // (a) ni dos menuitem en la misma línea ni uno partido en varias líneas
            if (apariciones > 1) {
                v.add(new Violacion(fichero, ubicacion, "hay " + apariciones + " <menuitem> en la misma línea"));
                continue;
            }
            int cierre = linea.indexOf("/>", inicioTag);
            if (cierre < 0) {
                v.add(new Violacion(fichero, ubicacion,
                        "el <menuitem> está partido en varias líneas (la línea no contiene su \"/>\" de cierre)"));
                continue;
            }
            String tag = linea.substring(inicioTag, cierre + 2);

            // (b) sin dobles espacios de alineación y atributos en el orden canónico
            if (tag.contains("\t")) {
                v.add(new Violacion(fichero, ubicacion, "hay un tabulador dentro del tag <menuitem>"));
            }
            if (tag.contains("  ")) {
                v.add(new Violacion(fichero, ubicacion,
                        "hay dobles espacios de alineación dentro del tag <menuitem>"
                                + " (los atributos se separan con UN solo espacio)"));
            }
            int ultimoIndice = -1;
            String ultimoAtributo = null;
            Matcher ma = atributo.matcher(tag);
            while (ma.find()) {
                String nombreAtributo = ma.group(1);
                int indice = ORDEN_ATRIBUTOS.indexOf(nombreAtributo);
                if (indice < 0) {
                    continue; // atributo fuera del vocabulario canónico: sin orden fijado por la regla
                }
                if (indice <= ultimoIndice) {
                    v.add(new Violacion(fichero, ubicacion,
                            "el atributo " + nombreAtributo + " aparece después de " + ultimoAtributo
                                    + " (orden canónico: " + String.join(", ", ORDEN_ATRIBUTOS) + ")"));
                }
                if (indice > ultimoIndice) {
                    ultimoIndice = indice;
                    ultimoAtributo = nombreAtributo;
                }
            }

            // (c) sangría = 4 × profundidad (resuelta con el árbol name/parent del DOM)
            Matcher mn = atributoName.matcher(tag);
            if (!mn.find()) {
                continue; // sin name no se puede resolver la profundidad; VAR-10.1 ya lo señala
            }
            String nombre = mn.group(1);
            int profundidad = profundidad(nombre, padrePorNombre);
            if (profundidad < 0) {
                v.add(new Violacion(fichero, "línea " + numLinea + " (" + nombre + ")",
                        "no se puede resolver su profundidad: parent no declarado o ciclo en la cadena de parent"));
                continue;
            }
            int esperada = 4 * profundidad;
            if (inicioTag != esperada || !linea.substring(0, inicioTag).chars().allMatch(c -> c == ' ')) {
                v.add(new Violacion(fichero, "línea " + numLinea + " (" + nombre + ")",
                        "sangría incorrecta: se esperaban " + esperada + " espacios (4 × profundidad "
                                + profundidad + ") y el tag empieza en la columna " + inicioTag));
            }
        }
    }

    /**
     * Líneas de menus.xml con el contenido de los comentarios XML ({@code <!-- … -->}, también
     * multilínea) sustituido por espacios: un menuitem comentado no es un menuitem (el DOM tampoco
     * lo ve), pero así se conservan la numeración de líneas y las columnas del resto del texto.
     */
    private static List<String> lineasSinComentarios(List<String> lineasCrudas) {
        List<String> out = new ArrayList<>();
        boolean dentro = false;
        for (String linea : lineasCrudas) {
            StringBuilder sb = new StringBuilder(linea.length());
            int i = 0;
            while (i < linea.length()) {
                if (!dentro && linea.startsWith("<!--", i)) {
                    dentro = true;
                    sb.append("    ");
                    i += 4;
                } else if (dentro && linea.startsWith("-->", i)) {
                    dentro = false;
                    sb.append("   ");
                    i += 3;
                } else {
                    sb.append(dentro ? ' ' : linea.charAt(i));
                    i++;
                }
            }
            out.add(sb.toString());
        }
        return out;
    }

    /**
     * VAR-10.4 — Naming {@code -menuitem}: todo name termina en {@code -menuitem};
     * y todo menuitem con parent prefija su name con el name del padre sin el sufijo
     * {@code -menuitem} (más un guion).
     */
    // [VAR-10.4] Verificación:
    //   Sujeto: cada `<menuitem>`.
    //   Condición: `name` termina en `-menuitem`;
    //     las hojas prefijan el nombre del padre (sin su sufijo `-menuitem`).
    //   Exenciones: los `<menuitem>` de `hide-menus.xml`, cuyo `name` es el del menú de Axelor que ocultan (ver la exención de categoría y `VAR-10.5`).
    @Test
    void var10_4_namingMenuitemYPrefijoDelPadre() {
        List<Violacion> v = new ArrayList<>();
        // Exención: hide-menus.xml queda fuera del sujeto de esta regla.
        for (Element mi : ViewFiles.byTag(ViewFiles.menusDoc(), "menuitem")) {
            String nombre = attr(mi, "name");
            String ubicacion = nombre.isEmpty() ? "<menuitem sin name>" : nombre;
            if (!nombre.endsWith("-menuitem")) {
                v.add(new Violacion(MENUS, ubicacion, "el name no termina en \"-menuitem\""));
            }
            if (hasAttr(mi, "parent")) {
                String parent = attr(mi, "parent");
                String base = parent.endsWith("-menuitem")
                        ? parent.substring(0, parent.length() - "-menuitem".length())
                        : parent;
                String prefijo = base + "-";
                if (!nombre.startsWith(prefijo)) {
                    v.add(new Violacion(MENUS, ubicacion,
                            "el name no empieza por el prefijo de su padre \"" + prefijo
                                    + "\" (padre: " + parent + ")"));
                }
            }
        }
        Violacion.assertNone(
                "VAR-10.4 — name termina en -menuitem y las hojas prefijan el name de su padre",
                v);
    }

    /**
     * VAR-10.5 — hide-menus.xml solo oculta: todo menuitem suyo lleva hidden="true" y un id que
     * empieza por "secretariaVirtual-"; y ningún menuitem de menus.xml lleva hidden="true".
     */
    // [VAR-10.5] Verificación:
    //   Sujeto: cada `<menuitem>` de los dos ficheros de menús.
    //   Condición:
    //     (a) si está en `hide-menus.xml` ⇒ tiene `hidden="true"` y un `id` que empieza por `secretariaVirtual-`;
    //     (b) si tiene `hidden="true"` ⇒ está en `hide-menus.xml` (no se ocultan menús desde `menus.xml`).
    @Test
    void var10_5_hideMenusSoloOculta() {
        List<Violacion> v = new ArrayList<>();

        // (a) todo menuitem de hide-menus.xml oculta, y con id propio
        for (Element mi : ViewFiles.byTag(ViewFiles.hideMenusDoc(), "menuitem")) {
            String ubicacion = hasAttr(mi, "name") ? attr(mi, "name") : "<menuitem sin name>";
            if (!"true".equals(attr(mi, "hidden"))) {
                v.add(new Violacion(HIDE_MENUS, ubicacion,
                        "en hide-menus.xml todo <menuitem> debe llevar hidden=\"true\": este fichero"
                                + " solo oculta menús de Axelor, no declara menús de la aplicación"));
            }
            if (!attr(mi, "id").startsWith(PREFIJO_ID_OCULTACION)) {
                v.add(new Violacion(HIDE_MENUS, ubicacion,
                        "el id debe existir y empezar por \"" + PREFIJO_ID_OCULTACION + "\" (id=\""
                                + attr(mi, "id") + "\"); sin un id propio, ViewLoader.importMenu no sube"
                                + " la prioridad sobre el menú de Axelor y la ocultación no surte efecto"));
            }
        }

        // (b) menus.xml no oculta nada
        for (Element mi : ViewFiles.byTag(ViewFiles.menusDoc(), "menuitem")) {
            if ("true".equals(attr(mi, "hidden"))) {
                String ubicacion = hasAttr(mi, "name") ? attr(mi, "name") : "<menuitem sin name>";
                v.add(new Violacion(MENUS, ubicacion,
                        "los <menuitem> con hidden=\"true\" van en hide-menus.xml, no en menus.xml"));
            }
        }

        Violacion.assertNone(
                "VAR-10.5 — En hide-menus.xml todo <menuitem> lleva hidden=\"true\" e id que empieza por \""
                        + PREFIJO_ID_OCULTACION + "\"; menus.xml no oculta nada",
                v);
    }

    // ---- helpers ----

    /** Mapa name → parent de todos los menuitem de un fichero de menús ("" si no tiene parent). */
    private static Map<String, String> padrePorNombre(org.w3c.dom.Document doc) {
        Map<String, String> m = new HashMap<>();
        for (Element mi : ViewFiles.byTag(doc, "menuitem")) {
            String nombre = attr(mi, "name");
            if (!nombre.isEmpty()) {
                m.put(nombre, attr(mi, "parent"));
            }
        }
        return m;
    }

    /**
     * Profundidad de un menuitem en el árbol name/parent: 0 si no tiene parent,
     * profundidad(padre)+1 si lo tiene. Devuelve -1 si el parent no está declarado
     * o hay un ciclo en la cadena de parent.
     */
    private static int profundidad(String nombre, Map<String, String> padrePorNombre) {
        int nivel = 0;
        String actual = nombre;
        // Cota dura contra ciclos: nunca puede haber más niveles que menuitems declarados.
        int limite = padrePorNombre.size() + 1;
        while (true) {
            String parent = padrePorNombre.get(actual);
            if (parent == null) {
                return -1; // menuitem (o parent intermedio) no declarado
            }
            if (parent.isEmpty()) {
                return nivel;
            }
            actual = parent;
            nivel++;
            if (nivel > limite) {
                return -1; // ciclo en la cadena de parent
            }
        }
    }
}
