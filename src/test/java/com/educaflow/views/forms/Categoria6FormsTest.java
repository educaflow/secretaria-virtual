// =====================================================================
// GENERADO por /code-create-view-tests desde agent_docs/view-rules.md
// NO EDITAR A MANO. Para cambiar un test, edita view-rules.md (o corrige
// la traducción en el skill /code-create-view-tests) y vuelve a ejecutarlo.
// =====================================================================
package com.educaflow.views.forms;

import com.educaflow.views.support.Index;
import com.educaflow.views.support.NombreVista;
import com.educaflow.views.support.ViewFile;
import com.educaflow.views.support.ViewFiles;
import com.educaflow.views.support.Violacion;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import static com.educaflow.views.support.ViewFiles.attr;
import static com.educaflow.views.support.ViewFiles.byTag;
import static com.educaflow.views.support.ViewFiles.childrenByTag;
import static com.educaflow.views.support.ViewFiles.hasAttr;

/**
 * Categoría 6 — Forms (agent_docs/view-rules.md).
 *
 * <p>Reglas estructurales de los forms; los atributos canónicos los verifica la Categoría 5.
 * La clase de bloque (maestro/detalle/referencia) se deduce mecánicamente del name
 * ({@link NombreVista}); un <b>detalle de solo lectura</b> es un form de detalle sin ningún
 * {@code <button>} y queda exento de las reglas de botones y de {@code onNew}.
 */
class Categoria6FormsTest {

    // ---------------------------------------------------------------- VAR-6.1

    // [VAR-6.1] Verificación:
    //   Sujeto: cada `<form>`.
    //   Condición:
    //     (a) los forms de clase **maestro** y **referencia** contienen exactamente un `<panel name="buttons-panel">`;
    //       los de **detalle** también, salvo los de solo lectura (sin botones);
    //     (b) todo `buttons-panel` contiene 1..n `<button>` cuyo `name` empieza por `btn` (sus atributos canónicos los fija `VAR-5.1`);
    //     (c) ningún `<button>` del form vive fuera del `buttons-panel`.
    @Test
    void var6_1_panelDeBotones() {
        List<Violacion> v = new ArrayList<>();
        for (ViewFile vf : ViewFiles.all()) {
            for (Element form : vf.forms()) {
                String name = attr(form, "name");
                List<Element> botonesForm = byTag(form, "button");
                List<Element> buttonsPanels = byTag(form, "panel").stream()
                        .filter(p -> "buttons-panel".equals(attr(p, "name")))
                        .toList();

                // (a) exactamente un buttons-panel según la clase; los forms sin clase
                // (name que no parsea: overrides Axelor) quedan fuera de esta condición.
                NombreVista nv = NombreVista.parse(name);
                if (nv != null) {
                    boolean requerido = switch (nv.clase()) {
                        case MAESTRO, REFERENCIA -> true;
                        // detalle: también, salvo detalle de solo lectura (sin ningún botón)
                        case DETALLE -> !botonesForm.isEmpty();
                    };
                    if (requerido && buttonsPanels.size() != 1) {
                        v.add(new Violacion(vf.rel(), name,
                                "(a) form de clase " + nv.clase() + ": debe contener exactamente un "
                                        + "<panel name=\"buttons-panel\"> y tiene " + buttonsPanels.size()));
                    }
                }

                // (b) todo buttons-panel contiene 1..n <button> cuyo name empieza por btn
                Set<Element> botonesEnPanel = new HashSet<>();
                for (Element panel : buttonsPanels) {
                    List<Element> botones = byTag(panel, "button");
                    botonesEnPanel.addAll(botones);
                    if (botones.isEmpty()) {
                        v.add(new Violacion(vf.rel(), name,
                                "(b) el buttons-panel no contiene ningún <button> (debe tener 1..n)"));
                    }
                    for (Element b : botones) {
                        if (!attr(b, "name").startsWith("btn")) {
                            v.add(new Violacion(vf.rel(), name,
                                    "(b) el botón \"" + attr(b, "name")
                                            + "\" del buttons-panel no empieza por \"btn\""));
                        }
                    }
                }

                // (c) ningún <button> del form fuera del buttons-panel
                for (Element b : botonesForm) {
                    if (!botonesEnPanel.contains(b)) {
                        v.add(new Violacion(vf.rel(), name,
                                "(c) el botón \"" + attr(b, "name") + "\" vive fuera del buttons-panel"));
                    }
                }
            }
        }
        Violacion.assertNone("VAR-6.1 — panel de botones (un buttons-panel por form según su clase, "
                + "con 1..n botones btn* y sin botones sueltos fuera)", v);
    }

    // ---------------------------------------------------------------- VAR-6.2

    // [VAR-6.2] Verificación:
    //   Sujeto: hijos directos de `<form>`.
    //   Condición: no hay `<field>` como hijo directo.
    @Test
    void var6_2_camposDentroDePanel() {
        List<Violacion> v = new ArrayList<>();
        for (ViewFile vf : ViewFiles.all()) {
            for (Element form : vf.forms()) {
                for (Element field : childrenByTag(form, "field")) {
                    v.add(new Violacion(vf.rel(), attr(form, "name"),
                            "el <field name=\"" + attr(field, "name")
                                    + "\"> es hijo directo del <form>; debe vivir dentro de un panel"));
                }
            }
        }
        Violacion.assertNone("VAR-6.2 — ningún <field> como hijo directo de <form> "
                + "(los campos van agrupados dentro de paneles)", v);
    }

    // ---------------------------------------------------------------- VAR-6.3

    // [VAR-6.3] Verificación:
    //   Sujeto: cada `<field>` con `form-view`/`grid-view`.
    //   Condición: el `form-view` acaba en `Ref@…-form` y el `grid-view` en `Ref@…-grid`
    //     (que además existen, por `VAR-4.1`).
    @Test
    void var6_3_camposRelacionalesApuntanARef() {
        List<Violacion> v = new ArrayList<>();
        for (ViewFile vf : ViewFiles.all()) {
            for (Element field : vf.byTag("field")) {
                String campo = attr(field, "name");
                if (hasAttr(field, "form-view")) {
                    String fv = attr(field, "form-view");
                    NombreVista nv = NombreVista.parse(fv);
                    if (nv == null || !"Ref".equals(nv.variante()) || !"form".equals(nv.tipo())) {
                        v.add(new Violacion(vf.rel(), "field " + campo,
                                "form-view=\"" + fv + "\" no es una vista de referencia (Ref@…-form)"));
                    }
                }
                if (hasAttr(field, "grid-view")) {
                    String gv = attr(field, "grid-view");
                    NombreVista nv = NombreVista.parse(gv);
                    if (nv == null || !"Ref".equals(nv.variante()) || !"grid".equals(nv.tipo())) {
                        v.add(new Violacion(vf.rel(), "field " + campo,
                                "grid-view=\"" + gv + "\" no es una vista de referencia (Ref@…-grid)"));
                    }
                }
            }
        }
        Violacion.assertNone("VAR-6.3 — los form-view/grid-view de los <field> relacionales apuntan "
                + "a las vistas de referencia (Ref@…-form / Ref@…-grid)", v);
    }

    // ---------------------------------------------------------------- VAR-6.4

    // [VAR-6.4] Verificación:
    //   Sujeto: cada `<form>` de clase detalle, salvo los de solo lectura.
    //   Condición, con `{campoPadre}` = el penúltimo segmento de la ruta de entidad del bloque en lowerCamelCase (`Correo.Adjunto` → `correo`):
    //     (a) tiene atributo `onNew`, y el `action-group` que referencia incluye una acción `{contexto}-set-{campoPadre}-parent-action`;
    //     (b) existe un `<field name="{campoPadre}" showIf="false">`.
    @Test
    void var6_4_detalleEnlazaCampoPadreConOnNew() {
        List<Violacion> v = new ArrayList<>();
        for (ViewFile vf : ViewFiles.all()) {
            for (Element form : vf.forms()) {
                String name = attr(form, "name");
                NombreVista nv = NombreVista.parse(name);
                if (nv == null || nv.clase() != NombreVista.Clase.DETALLE) {
                    continue;
                }
                // detalle de solo lectura (sin ningún <button>): exento
                if (byTag(form, "button").isEmpty()) {
                    continue;
                }
                // campoPadre = penúltimo segmento de la ruta de entidad, en lowerCamelCase
                String segmento = nv.rutaEntidad().get(nv.rutaEntidad().size() - 2);
                String campoPadre = Character.toLowerCase(segmento.charAt(0)) + segmento.substring(1);

                // (a) onNew presente y su action-group incluye {contexto}-set-{campoPadre}-parent-action
                String esperada = nv.contexto() + "-set-" + campoPadre + "-parent-action";
                if (!hasAttr(form, "onNew")) {
                    v.add(new Violacion(vf.rel(), name, "(a) form de detalle sin atributo onNew"));
                } else {
                    String onNew = attr(form, "onNew");
                    List<String> acciones = Index.accionesDeGrupo(vf, onNew);
                    if (!acciones.contains(esperada)) {
                        v.add(new Violacion(vf.rel(), name,
                                "(a) el action-group del onNew (\"" + onNew + "\") no incluye la acción \""
                                        + esperada + "\" (acciones: " + acciones + ")"));
                    }
                }

                // (b) existe <field name="{campoPadre}" showIf="false">
                boolean existe = byTag(form, "field").stream()
                        .anyMatch(f -> campoPadre.equals(attr(f, "name"))
                                && "false".equals(attr(f, "showIf")));
                if (!existe) {
                    v.add(new Violacion(vf.rel(), name,
                            "(b) falta el campo padre oculto <field name=\"" + campoPadre
                                    + "\" showIf=\"false\"/>"));
                }
            }
        }
        Violacion.assertNone("VAR-6.4 — el form de detalle enlaza el campo padre (oculto) con onNew", v);
    }

    // ---------------------------------------------------------------- VAR-6.5

    /** Nombres de panel genéricos prohibidos: panel1, Panel, nombrePanel3… */
    private static final Pattern PANEL_GENERICO = Pattern.compile("(?i)^(panel|nombrePanel)[0-9]*$");

    // [VAR-6.5] Verificación:
    //   Sujeto: cada `<panel>` con `name` (excepto `buttons-panel`).
    //   Condición: el `name` no casa con el patrón `(?i)^(panel|nombrePanel)[0-9]*$`.
    @Test
    void var6_5_nombresDePanelNoGenericos() {
        List<Violacion> v = new ArrayList<>();
        for (ViewFile vf : ViewFiles.all()) {
            for (Element panel : vf.byTag("panel")) {
                String name = attr(panel, "name");
                if (name.isEmpty() || "buttons-panel".equals(name)) {
                    continue; // sujeto: paneles con name, excepto el buttons-panel
                }
                if (PANEL_GENERICO.matcher(name).matches()) {
                    v.add(new Violacion(vf.rel(), "panel " + name,
                            "name de panel genérico; debe ser semántico (entidad o rol del grupo)"));
                }
            }
        }
        Violacion.assertNone("VAR-6.5 — nombres de panel no genéricos "
                + "(no casan con (?i)^(panel|nombrePanel)[0-9]*$)", v);
    }
}
