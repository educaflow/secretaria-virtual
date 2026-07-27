// =====================================================================
// GENERADO por /developer-create-view-tests desde agent_docs/view-rules.md
// NO EDITAR A MANO. Para cambiar un test, edita view-rules.md (o corrige
// la traducción en el skill /developer-create-view-tests) y vuelve a ejecutarlo.
// =====================================================================
package com.educaflow.views.botones;

import com.educaflow.views.support.Index;
import com.educaflow.views.support.NombreVista;
import com.educaflow.views.support.ViewFile;
import com.educaflow.views.support.ViewFiles;
import com.educaflow.views.support.Violacion;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.educaflow.views.support.ViewFiles.attr;
import static com.educaflow.views.support.ViewFiles.byTag;
import static com.educaflow.views.support.ViewFiles.childrenByTag;
import static com.educaflow.views.support.ViewFiles.hasAttr;

/**
 * Categoría 7 — Botones y secuencias de acciones (agent_docs/view-rules.md).
 *
 * <p>Las secuencias de los botones estándar ({@code btnSave}/{@code btnDelete}/{@code btnCancel})
 * dependen de la clase del form: el maestro va al servidor, el detalle opera en la colección en
 * memoria del padre (acciones {@code -modal}, sin {@code remote-validation*}) y la referencia
 * solo cierra.
 */
class Categoria7BotonesTest {

    // ---------------------------------------------------------------- VAR-7.1

    // [VAR-7.1] Verificación:
    //   Sujeto: cada `<button>` de un `<form>` con `onClick`.
    //   El `onClick` designa uno o varios action-group; su **segmento canónico** es el último: en un `onClick` normal, el valor completo; en un `onClick` con prefijo `serial:`, el último segmento de la lista separada por comas.
    //   Condición:
    //     (a) el segmento canónico es `{contexto del bloque}-btn{X}-action` (su contexto es **el del bloque del form del botón**, su descripción empieza por `btn` y su tipo es `action`) y resuelve a un `<action-group>` (`VAR-4.1`);
    //     (b) el `name` del botón **empieza por** `btn{X}` (la descripción del segmento canónico);
    //     (c) si el `onClick` lleva prefijo `serial:`, **cada** segmento anterior resuelve también a un `<action-group>` cuyo `name` empieza por `{contexto}-btn{X}-` (mismo botón; descripción `btn{X}-{sufijo}`), de modo que todos los grupos encadenados quedan atados al botón.
    @Test
    void var7_1_onClickDeBotonResuelveASuActionGroup() {
        List<Violacion> v = new ArrayList<>();
        for (ViewFile vf : ViewFiles.all()) {
            for (Element form : vf.forms()) {
                String formName = attr(form, "name");
                // Contexto del bloque del form (null en los overrides Axelor, cuyo name no parsea).
                NombreVista nvForm = NombreVista.parse(formName);
                for (Element btn : byTag(form, "button")) {
                    String onClick = attr(btn, "onClick");
                    if (onClick.isEmpty()) {
                        continue; // sujeto: solo botones con onClick
                    }
                    String btnName = attr(btn, "name");
                    String ubicacion = formName + " > " + btnName;

                    // El onClick designa uno o varios action-group. Con prefijo serial:
                    // (AutoFirma) el segmento CANÓNICO es el último de la lista separada por
                    // comas y los anteriores son grupos intermedios que deben esperarse; en un
                    // onClick normal, el segmento canónico es el valor completo.
                    boolean serial = onClick.startsWith("serial:");
                    String[] segmentos = serial
                            ? onClick.substring("serial:".length()).split(",")
                            : new String[] { onClick };
                    for (int i = 0; i < segmentos.length; i++) {
                        segmentos[i] = segmentos[i].trim();
                    }
                    String canonico = segmentos[segmentos.length - 1];

                    // (a) el segmento canónico es {contexto}-btn{X}-action y resuelve a un <action-group>
                    NombreVista nvClick = NombreVista.parse(canonico);
                    if (nvClick == null || !nvClick.descripcion().startsWith("btn")
                            || !"action".equals(nvClick.tipo())) {
                        v.add(new Violacion(vf.rel(), ubicacion,
                                "(a) el segmento canónico \"" + canonico
                                        + "\" no sigue el patrón {contexto}-btn{X}-action"));
                        continue;
                    }
                    if (!Index.grupos().contains(canonico)) {
                        v.add(new Violacion(vf.rel(), ubicacion,
                                "(a) el segmento canónico \"" + canonico
                                        + "\" no resuelve a ningún <action-group>"));
                    }
                    if (nvForm != null && !nvClick.contexto().equals(nvForm.contexto())) {
                        v.add(new Violacion(vf.rel(), ubicacion,
                                "(a) el segmento canónico \"" + canonico + "\" es de contexto "
                                        + nvClick.contexto() + ", distinto del contexto del bloque del form ("
                                        + nvForm.contexto() + ")"));
                    }

                    // (b) el name del botón empieza por el btn{X} del segmento canónico
                    if (!btnName.startsWith(nvClick.descripcion())) {
                        v.add(new Violacion(vf.rel(), ubicacion,
                                "(b) el name \"" + btnName + "\" no empieza por \""
                                        + nvClick.descripcion() + "\" (la descripción de su onClick)"));
                    }

                    // (c) en un serial:, cada segmento anterior resuelve también a un
                    //     <action-group> cuyo name empieza por {contexto}-btn{X}- (mismo botón)
                    if (serial) {
                        String prefijoBoton = nvClick.contexto() + "-" + nvClick.descripcion() + "-";
                        for (int i = 0; i < segmentos.length - 1; i++) {
                            String previo = segmentos[i];
                            if (!Index.grupos().contains(previo)) {
                                v.add(new Violacion(vf.rel(), ubicacion,
                                        "(c) el segmento serial: \"" + previo
                                                + "\" no resuelve a ningún <action-group>"));
                            }
                            if (!previo.startsWith(prefijoBoton)) {
                                v.add(new Violacion(vf.rel(), ubicacion,
                                        "(c) el segmento serial: \"" + previo
                                                + "\" no empieza por \"" + prefijoBoton
                                                + "\" (el prefijo del botón)"));
                            }
                        }
                    }
                }
            }
        }
        Violacion.assertNone("VAR-7.1 — el onClick de cada botón es su action-group btn{X} del "
                + "bloque de su form (y el name del botón empieza por ese btn{X}); en serial:, "
                + "todos los segmentos son action-group del mismo botón", v);
    }

    // ---------------------------------------------------------------- VAR-7.2

    // [VAR-7.2] Verificación:
    //   Sujeto: el `<action-group>` referenciado por el `onClick` de cada botón estándar (`name` que empieza por `btnSave`/`btnDelete`/`btnCancel`), según la clase de su form —
    //
    //   | Botón | maestro | detalle | referencia |
    //   |---|---|---|---|
    //   | `btnSave` | [`Local-…`]* → `remote-validationSave-action` → `save` → `back`\|`force-back` (inmediatamente tras `save`) | [`Local-…`]* → `save-modal`; **sin** ninguna `remote-validation*` | no existe |
    //   | `btnDelete` | [`remote-validationDelete-action`] → `delete` (termina en `delete`) | termina en `delete-modal`; **sin** ninguna `remote-validation*` | no existe |
    //   | `btnCancel` | contiene `back` (o `force-back`) | contiene `close` | contiene `close` |
    @Test
    void var7_2_secuenciaDeBotonesEstandarSegunClase() {
        List<Violacion> v = new ArrayList<>();
        for (ViewFile vf : ViewFiles.all()) {
            for (Element form : vf.forms()) {
                String formName = attr(form, "name");
                NombreVista nv = NombreVista.parse(formName);
                if (nv == null) {
                    continue; // sin clase de bloque: fuera del sujeto
                }
                String ctx = nv.contexto();
                for (Element btn : byTag(form, "button")) {
                    String btnName = attr(btn, "name");
                    String estandar = btnName.startsWith("btnSave") ? "btnSave"
                            : btnName.startsWith("btnDelete") ? "btnDelete"
                            : btnName.startsWith("btnCancel") ? "btnCancel"
                            : null;
                    if (estandar == null) {
                        continue; // sujeto: solo los botones estándar
                    }
                    String ubicacion = formName + " > " + btnName;

                    // en la clase referencia btnSave/btnDelete no existen
                    if (nv.clase() == NombreVista.Clase.REFERENCIA && !"btnCancel".equals(estandar)) {
                        v.add(new Violacion(vf.rel(), ubicacion,
                                "un form de clase referencia no puede tener " + estandar));
                        continue;
                    }

                    List<String> seq = Index.accionesDeGrupo(vf, attr(btn, "onClick"));
                    switch (nv.clase()) {
                        case MAESTRO -> verificarMaestro(v, vf, ubicacion, estandar, ctx, seq);
                        case DETALLE -> verificarDetalle(v, vf, ubicacion, estandar, ctx, seq);
                        case REFERENCIA -> {
                            // solo llega btnCancel: contiene close
                            if (!seq.contains("close")) {
                                v.add(new Violacion(vf.rel(), ubicacion,
                                        "el btnCancel de una referencia debe contener \"close\"; "
                                                + "secuencia: " + seq));
                            }
                        }
                    }
                }
            }
        }
        Violacion.assertNone("VAR-7.2 — secuencia de los botones estándar según la clase del form "
                + "(maestro: validaciones→save→back; detalle: acciones -modal sin remote-validation*; "
                + "referencia: solo close)", v);
    }

    private static void verificarMaestro(List<Violacion> v, ViewFile vf, String ubicacion,
                                         String estandar, String ctx, List<String> seq) {
        switch (estandar) {
            case "btnSave" -> {
                // [Local-… del mismo contexto]* → remote-validationSave-action → save
                //   → back|force-back (inmediatamente tras save)
                List<String> resto = sinLocalesIniciales(seq, ctx);
                boolean ok = resto.size() == 3
                        && "remote-validationSave-action".equals(resto.get(0))
                        && "save".equals(resto.get(1))
                        && ("back".equals(resto.get(2)) || "force-back".equals(resto.get(2)));
                if (!ok) {
                    v.add(new Violacion(vf.rel(), ubicacion,
                            "el btnSave maestro debe ser [Local-…]* → remote-validationSave-action → "
                                    + "save → back|force-back; secuencia: " + seq));
                }
            }
            case "btnDelete" -> {
                // [remote-validationDelete-action] → delete (termina en delete)
                if (seq.isEmpty() || !"delete".equals(seq.get(seq.size() - 1))) {
                    v.add(new Violacion(vf.rel(), ubicacion,
                            "el btnDelete maestro debe terminar en \"delete\"; secuencia: " + seq));
                }
            }
            case "btnCancel" -> {
                if (!seq.contains("back") && !seq.contains("force-back")) {
                    v.add(new Violacion(vf.rel(), ubicacion,
                            "el btnCancel maestro debe contener \"back\" o \"force-back\"; "
                                    + "secuencia: " + seq));
                }
            }
            default -> throw new IllegalStateException(estandar);
        }
    }

    private static void verificarDetalle(List<Violacion> v, ViewFile vf, String ubicacion,
                                         String estandar, String ctx, List<String> seq) {
        // en el detalle no aplica ninguna validación remota (el walker valida en cascada)
        if (("btnSave".equals(estandar) || "btnDelete".equals(estandar))
                && seq.stream().anyMatch(a -> a.startsWith("remote-validation"))) {
            v.add(new Violacion(vf.rel(), ubicacion,
                    "el " + estandar + " de un detalle no puede incluir ninguna remote-validation*; "
                            + "secuencia: " + seq));
        }
        switch (estandar) {
            case "btnSave" -> {
                // [Local-… del mismo contexto]* → save-modal
                List<String> resto = sinLocalesIniciales(seq, ctx);
                if (!List.of("save-modal").equals(resto)) {
                    v.add(new Violacion(vf.rel(), ubicacion,
                            "el btnSave de un detalle debe ser [Local-…]* → save-modal; "
                                    + "secuencia: " + seq));
                }
            }
            case "btnDelete" -> {
                if (seq.isEmpty() || !"delete-modal".equals(seq.get(seq.size() - 1))) {
                    v.add(new Violacion(vf.rel(), ubicacion,
                            "el btnDelete de un detalle debe terminar en \"delete-modal\"; "
                                    + "secuencia: " + seq));
                }
            }
            case "btnCancel" -> {
                if (!seq.contains("close")) {
                    v.add(new Violacion(vf.rel(), ubicacion,
                            "el btnCancel de un detalle debe contener \"close\"; secuencia: " + seq));
                }
            }
            default -> throw new IllegalStateException(estandar);
        }
    }

    /** Quita del principio de la secuencia las acciones Local-… del mismo contexto. */
    private static List<String> sinLocalesIniciales(List<String> seq, String ctx) {
        int i = 0;
        while (i < seq.size() && seq.get(i).startsWith(ctx + "-Local-")) {
            i++;
        }
        return seq.subList(i, seq.size());
    }

    // ---------------------------------------------------------------- VAR-7.3

    /** Predefinidas admitidas en los grupos de btnSave/btnDelete (tabla de VAR-7.3). */
    private static final Set<String> PREDEFINIDAS_SAVE_DELETE = Set.of(
            "save", "delete", "back", "force-back", "save-modal", "delete-modal", "close");

    // [VAR-7.3] Verificación:
    //   Sujeto: los `<action name>` de cada `<action-group>` de `btnSave`/`btnDelete`.
    //   Condición: cada uno es —
    //     una acción `Local-…` del mismo contexto,
    //     una de las globales `remote-validationSave-action`/`remote-validationDelete-action`,
    //     o una predefinida (`save`, `delete`, `back`, `force-back`, `save-modal`, `delete-modal`, `close`).
    //   Ningún `Remote-…-action` propio.
    @Test
    void var7_3_gruposSaveDeleteSinControladoresPropios() {
        List<Violacion> v = new ArrayList<>();
        for (ViewFile vf : ViewFiles.all()) {
            for (Map.Entry<String, List<String>> grupo : vf.actionGroups().entrySet()) {
                NombreVista nv = NombreVista.parse(grupo.getKey());
                if (nv == null) {
                    continue;
                }
                String d = nv.descripcion();
                if (!d.startsWith("btnSave") && !d.startsWith("btnDelete")) {
                    continue; // sujeto: solo los grupos de btnSave/btnDelete
                }
                String ctx = nv.contexto();
                for (String accion : grupo.getValue()) {
                    boolean ok = accion.startsWith(ctx + "-Local-")
                            || "remote-validationSave-action".equals(accion)
                            || "remote-validationDelete-action".equals(accion)
                            || PREDEFINIDAS_SAVE_DELETE.contains(accion);
                    if (!ok) {
                        v.add(new Violacion(vf.rel(), grupo.getKey(),
                                "la acción \"" + accion + "\" no está admitida en un grupo de "
                                        + "save/delete (solo Local-… del mismo contexto, "
                                        + "remote-validationSave/Delete-action o predefinidas "
                                        + PREDEFINIDAS_SAVE_DELETE + "); ningún Remote-… propio"));
                    }
                }
            }
        }
        Violacion.assertNone("VAR-7.3 — los grupos de save/delete no llaman a controladores propios "
                + "(la persistencia la hace siempre la plataforma)", v);
    }

    // ---------------------------------------------------------------- VAR-7.4

    /** Acción remota propia: {prefijo}-Remote-{Op}-action. */
    private static final Pattern REMOTE_OP = Pattern.compile("^(.*-Remote-)([A-Za-z0-9]+)-action$");

    // [VAR-7.4] Verificación:
    //   Sujeto: cada `<action-group>` que contenga una acción `…-Remote-{Op}-action` (con `{Op}` que no empiece por `validate`).
    //   Condición: si existe (en el ámbito) la acción `…-Remote-validate{Op}-action` del mismo contexto,
    //     el grupo la incluye **inmediatamente antes** de `…-Remote-{Op}-action` (`{Op}` capitalizado tras `validate`).
    @Test
    void var7_4_remoteValidateOpInmediatamenteAntesDeRemoteOp() {
        List<Violacion> v = new ArrayList<>();
        Set<String> declaradas = Index.accionesDeclaradas();
        for (ViewFile vf : ViewFiles.all()) {
            for (Map.Entry<String, List<String>> grupo : vf.actionGroups().entrySet()) {
                List<String> seq = grupo.getValue();
                for (int i = 0; i < seq.size(); i++) {
                    Matcher m = REMOTE_OP.matcher(seq.get(i));
                    if (!m.matches()) {
                        continue;
                    }
                    String op = m.group(2);
                    if (op.startsWith("validate")) {
                        continue; // sujeto: solo las operaciones, no sus validaciones
                    }
                    // validador emparejado por nombre, del mismo contexto
                    String validador = m.group(1) + "validate"
                            + Character.toUpperCase(op.charAt(0)) + op.substring(1) + "-action";
                    if (!declaradas.contains(validador)) {
                        continue; // no existe en el ámbito: la regla no aplica
                    }
                    if (i == 0 || !validador.equals(seq.get(i - 1))) {
                        v.add(new Violacion(vf.rel(), grupo.getKey(),
                                "existe \"" + validador + "\" y el grupo debe incluirla "
                                        + "inmediatamente antes de \"" + seq.get(i)
                                        + "\"; secuencia: " + seq));
                    }
                }
            }
        }
        Violacion.assertNone("VAR-7.4 — Remote-validate{Op} inmediatamente antes de Remote-{Op} "
                + "(toda operación custom valida en servidor justo antes de ejecutarse)", v);
    }

    // ---------------------------------------------------------------- VAR-7.5

    // [VAR-7.5] Verificación:
    //   Sujeto: cada `<panel name="buttons-panel">` y sus `<button>` **hijos directos**
    //     (los botones dentro de paneles anidados del `buttons-panel` quedan fuera del sujeto).
    //   Condición:
    //     (a) la suma de `colOffset` + `colSpan` de **todos** los botones hijos directos es ≤ 12
    //       (ocultos incluidos, porque reservan su sitio; `colSpan` ausente cuenta como 6 —el valor por defecto de Axelor— y `colOffset` ausente como 0);
    //     (b) todo botón hijo directo con `showIf` cumple:
    //       no tiene `colOffset`,
    //       y todos sus hermanos `<button>` anteriores del panel llevan también `showIf` sin `colOffset`
    //       (los condicionales solo pueden ser el tramo inicial pegado al borde izquierdo).
    //   El `showIf` canónico de `btnDelete*` (`VAR-5.1`) no está exento: si convive con gemelos u offsets condicionales, va dentro de su panel de estado (donde deja de ser hijo directo).
    @Test
    void var7_5_sumaDeColumnasDelPanelPlanoNoSupera12() {
        List<Violacion> v = new ArrayList<>();
        for (ViewFile vf : ViewFiles.all()) {
            for (Element form : vf.forms()) {
                String formName = attr(form, "name");
                for (Element panel : byTag(form, "panel")) {
                    if (!"buttons-panel".equals(attr(panel, "name"))) {
                        continue;
                    }
                    // (a) todos los hijos directos, ocultos incluidos: reservan su sitio en la fila
                    int total = 0;
                    for (Element btn : childrenByTag(panel, "button")) {
                        total += intAttr(btn, "colOffset", 0) + intAttr(btn, "colSpan", 6);
                    }
                    if (total > 12) {
                        v.add(new Violacion(vf.rel(), formName + " > buttons-panel",
                                "(a) los botones hijos directos declaran " + total + " columnas (> 12): "
                                        + "los ocultos reservan su sitio y los visibles saltan de fila; "
                                        + "agrupa los botones de cada estado en paneles anidados con el showIf en el panel"));
                    }
                }
            }
        }
        Violacion.assertNone("VAR-7.5 — botones condicionales en paneles de estado, no gemelos en panel "
                + "plano (la suma de colOffset+colSpan de los botones hijos directos del buttons-panel "
                + "no supera 12)", v);
    }

    // [VAR-7.5] (continuación)
    @Test
    void var7_5_condicionalesSoloComoTramoInicialSinColOffset() {
        List<Violacion> v = new ArrayList<>();
        for (ViewFile vf : ViewFiles.all()) {
            for (Element form : vf.forms()) {
                String formName = attr(form, "name");
                for (Element panel : byTag(form, "panel")) {
                    if (!"buttons-panel".equals(attr(panel, "name"))) {
                        continue;
                    }
                    List<Element> botones = childrenByTag(panel, "button");
                    for (int i = 0; i < botones.size(); i++) {
                        Element btn = botones.get(i);
                        if (!hasAttr(btn, "showIf")) {
                            continue; // sujeto de (b): solo botones hijos directos con showIf
                        }
                        String ubicacion = formName + " > " + attr(btn, "name");
                        if (hasAttr(btn, "colOffset")) {
                            v.add(new Violacion(vf.rel(), ubicacion,
                                    "(b) botón condicional (showIf) con colOffset: oculto deja un hueco "
                                            + "en medio de la fila; muévelo a un panel de estado anidado"));
                        }
                        for (int j = 0; j < i; j++) {
                            Element previo = botones.get(j);
                            if (!hasAttr(previo, "showIf") || hasAttr(previo, "colOffset")) {
                                v.add(new Violacion(vf.rel(), ubicacion,
                                        "(b) botón condicional (showIf) que no es tramo inicial del panel: "
                                                + "su hermano anterior \"" + attr(previo, "name")
                                                + "\" no lleva showIf sin colOffset; al ocultarse dejaría "
                                                + "un hueco en medio; muévelo a un panel de estado anidado"));
                                break;
                            }
                        }
                    }
                }
            }
        }
        Violacion.assertNone("VAR-7.5 — botones condicionales en paneles de estado, no gemelos en panel "
                + "plano (los condicionales solo pueden ser el tramo inicial sin colOffset del "
                + "buttons-panel)", v);
    }

    private static int intAttr(Element e, String name, int def) {
        String s = attr(e, name);
        if (s.isEmpty()) {
            return def;
        }
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException ex) {
            return def;
        }
    }
}
