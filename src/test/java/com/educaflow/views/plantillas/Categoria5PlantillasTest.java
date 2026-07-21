// =====================================================================
// GENERADO por /create-view-test desde agent_docs/view-rules.md
// NO EDITAR A MANO. Para cambiar un test, edita view-rules.md (o corrige
// la traducción en el skill /create-view-test) y vuelve a ejecutarlo.
// =====================================================================
package com.educaflow.views.plantillas;

import com.educaflow.views.support.NombreVista;
import com.educaflow.views.support.ViewFile;
import com.educaflow.views.support.ViewFiles;
import com.educaflow.views.support.Violacion;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;

import static com.educaflow.views.support.ViewFiles.attr;
import static com.educaflow.views.support.ViewFiles.byTag;
import static com.educaflow.views.support.ViewFiles.hasAttr;

/**
 * Categoría 5 — Plantillas canónicas (agent_docs/view-rules.md).
 *
 * <p>VAR-5.1 fija por tipo de elemento la plantilla de atributos que toda vista repite
 * (un test por fila de la tabla: form, grid, panel-related, buttons-panel y btnDelete);
 * VAR-5.2 fija que la clase referencia (variante {@code Ref}) es de solo lectura.
 */
class Categoria5PlantillasTest {

    /** Atributos can* de la fila form de VAR-5.1: si están presentes deben valer "false". */
    private static final List<String> CAN_FORM =
            List.of("canAttach", "canBack", "canDelete", "canNew", "canSave", "canMore");

    // ---------------------------------------------------------------- VAR-5.1 — fila <form>

    // [VAR-5.1] Verificación:
    //   Sujeto: cada `<form>`, `<grid>`, `<panel-related>`, `<panel name="buttons-panel">` y `<button>` cuyo `name` empieza por `btnDelete` — de cualquier clase, variante y nivel.
    //   Condición: el elemento cumple la fila de su tipo —
    //
    //   | Elemento | Atributos canónicos |
    //   |---|---|
    //   | `<form>` | `canAttach`/`canBack`/`canDelete`/`canNew`/`canSave`/`canMore`, si están presentes, valen `false` (ninguno vale `true`); sin atributo `onSave`; `canBackOnSave="true"` **solo** en el form maestro con `btnSave` (ausente en el maestro de solo consulta, el detalle y la referencia) |
    //   | `<grid>` | presentes `editable="false"`, `edit-icon="false"`, `x-selector="none"`, `canEdit="false"`, `canDelete="false"`, `canSave="false"`, `title=""` y `orderBy`; `canAdvanceSearch` y `canRefresh` ausentes o a `"false"`; sin atributo `archived` |
    //   | `<panel-related>` | presentes `colSpan="12"`, `showFooter="false"`, `canEdit="false"`, `canRemove="false"` y `forceEdit="true"` (la coherencia `canNew`/`newButtonTitle` la verifica `VAR-8.2`; que sus `grid-view`/`form-view` existan, `VAR-4.1`) |
    //   | `buttons-panel` | `title` vacío, `colSpan="12"` y `showFrame="false"` |
    //   | `btnDelete…` | `showIf="(id!=null) \|\| (cid!=null)"` (tolerando espacios), `css="btn-danger"` y `outline="true"` |
    @Test
    void var5_1_formAtributosCanonicos() {
        List<Violacion> v = new ArrayList<>();
        for (ViewFile vf : ViewFiles.all()) {
            for (Element form : vf.forms()) {
                String name = attr(form, "name");

                // can* presentes => "false" (ninguno puede valer "true")
                for (String a : CAN_FORM) {
                    if (hasAttr(form, a) && !"false".equals(attr(form, a))) {
                        v.add(new Violacion(vf.rel(), name,
                                a + "=\"" + attr(form, a) + "\": si está presente debe valer \"false\""));
                    }
                }

                // sin atributo onSave: la validación cuelga del btnSave-action, no del onSave
                if (hasAttr(form, "onSave")) {
                    v.add(new Violacion(vf.rel(), name,
                            "tiene onSave=\"" + attr(form, "onSave")
                                    + "\": el form no debe llevar atributo onSave"));
                }

                // canBackOnSave depende de la clase de bloque: los forms cuyo name no parsea
                // (overrides Axelor, sin '@') quedan exentos de esta fila.
                NombreVista nv = NombreVista.parse(name);
                if (nv == null) {
                    continue;
                }
                boolean tieneBtnSave = byTag(form, "button").stream()
                        .anyMatch(b -> attr(b, "name").startsWith("btnSave"));
                boolean requerido = nv.clase() == NombreVista.Clase.MAESTRO && tieneBtnSave;
                if (requerido) {
                    if (!"true".equals(attr(form, "canBackOnSave"))) {
                        v.add(new Violacion(vf.rel(), name,
                                "form maestro con btnSave: falta canBackOnSave=\"true\""));
                    }
                } else if (hasAttr(form, "canBackOnSave")) {
                    v.add(new Violacion(vf.rel(), name,
                            "canBackOnSave debe estar ausente (solo lo lleva el form maestro con btnSave; "
                                    + "este es " + nv.clase() + (tieneBtnSave ? " con" : " sin") + " btnSave)"));
                }
            }
        }
        Violacion.assertNone("VAR-5.1 — núcleo canónico de atributos del <form> "
                + "(can* a false, sin onSave, canBackOnSave solo en el maestro con btnSave)", v);
    }

    // ---------------------------------------------------------------- VAR-5.1 — fila <grid>

    // [VAR-5.1] (continuación)
    @Test
    void var5_1_gridAtributosCanonicos() {
        List<Violacion> v = new ArrayList<>();
        for (ViewFile vf : ViewFiles.all()) {
            for (Element grid : vf.grids()) {
                String name = attr(grid, "name");

                // atributos presentes con valor fijo
                exigir(v, vf, name, grid, "editable", "false");
                exigir(v, vf, name, grid, "edit-icon", "false");
                exigir(v, vf, name, grid, "x-selector", "none");
                exigir(v, vf, name, grid, "canEdit", "false");
                exigir(v, vf, name, grid, "canDelete", "false");
                exigir(v, vf, name, grid, "canSave", "false");

                // title presente y vacío
                if (!hasAttr(grid, "title") || !attr(grid, "title").isEmpty()) {
                    v.add(new Violacion(vf.rel(), name,
                            "debe llevar title=\"\" (presente y vacío); "
                                    + (hasAttr(grid, "title")
                                        ? "vale \"" + attr(grid, "title") + "\"" : "está ausente")));
                }

                // orderBy presente y no vacío
                if (!hasAttr(grid, "orderBy") || attr(grid, "orderBy").isEmpty()) {
                    v.add(new Violacion(vf.rel(), name, "falta el atributo orderBy (presente y no vacío)"));
                }

                // canAdvanceSearch / canRefresh ausentes o "false"
                for (String a : List.of("canAdvanceSearch", "canRefresh")) {
                    if (hasAttr(grid, a) && !"false".equals(attr(grid, a))) {
                        v.add(new Violacion(vf.rel(), name,
                                a + "=\"" + attr(grid, a) + "\": debe estar ausente o valer \"false\""));
                    }
                }

                // sin atributo archived
                if (hasAttr(grid, "archived")) {
                    v.add(new Violacion(vf.rel(), name, "no debe llevar el atributo archived"));
                }
            }
        }
        Violacion.assertNone("VAR-5.1 — núcleo canónico de atributos del <grid> "
                + "(solo consulta, ordenado, sin título, sin búsqueda avanzada/refresco ni archived)", v);
    }

    // ---------------------------------------------------------------- VAR-5.1 — fila <panel-related>

    // [VAR-5.1] (continuación)
    @Test
    void var5_1_panelRelatedAtributosCanonicos() {
        List<Violacion> v = new ArrayList<>();
        for (ViewFile vf : ViewFiles.all()) {
            for (Element pr : vf.byTag("panel-related")) {
                String name = attr(pr, "name");
                exigir(v, vf, name, pr, "colSpan", "12");
                exigir(v, vf, name, pr, "showFooter", "false");
                exigir(v, vf, name, pr, "canEdit", "false");
                exigir(v, vf, name, pr, "canRemove", "false");
                exigir(v, vf, name, pr, "forceEdit", "true");
            }
        }
        Violacion.assertNone("VAR-5.1 — núcleo canónico de atributos del <panel-related> "
                + "(ancho completo, sin edición en línea ni borrado directo, abre en edición)", v);
    }

    // ---------------------------------------------------------------- VAR-5.1 — fila buttons-panel

    // [VAR-5.1] (continuación)
    @Test
    void var5_1_buttonsPanelAtributosCanonicos() {
        List<Violacion> v = new ArrayList<>();
        for (ViewFile vf : ViewFiles.all()) {
            for (Element panel : vf.byTag("panel")) {
                if (!"buttons-panel".equals(attr(panel, "name"))) {
                    continue;
                }
                String ubicacion = ubicacionEnFichero(vf, panel);
                if (!hasAttr(panel, "title") || !attr(panel, "title").isEmpty()) {
                    v.add(new Violacion(vf.rel(), ubicacion,
                            "el buttons-panel debe llevar title=\"\" (presente y vacío)"));
                }
                exigir(v, vf, ubicacion, panel, "colSpan", "12");
                exigir(v, vf, ubicacion, panel, "showFrame", "false");
            }
        }
        Violacion.assertNone("VAR-5.1 — núcleo canónico de atributos del buttons-panel "
                + "(title vacío, colSpan=12, sin marco)", v);
    }

    // ---------------------------------------------------------------- VAR-5.1 — fila btnDelete

    // [VAR-5.1] (continuación)
    @Test
    void var5_1_btnDeleteAtributosCanonicos() {
        List<Violacion> v = new ArrayList<>();
        for (ViewFile vf : ViewFiles.all()) {
            for (Element btn : vf.byTag("button")) {
                String name = attr(btn, "name");
                if (!name.startsWith("btnDelete")) {
                    continue;
                }
                String ubicacion = ubicacionEnFichero(vf, btn) + " > " + name;

                // showIf="(id!=null) || (cid!=null)" comparando sin espacios
                String showIf = attr(btn, "showIf").replaceAll("\\s", "");
                if (!"(id!=null)||(cid!=null)".equals(showIf)) {
                    v.add(new Violacion(vf.rel(), ubicacion,
                            "showIf debe ser \"(id!=null) || (cid!=null)\"; "
                                    + (hasAttr(btn, "showIf")
                                        ? "vale \"" + attr(btn, "showIf") + "\"" : "está ausente")));
                }
                exigir(v, vf, ubicacion, btn, "css", "btn-danger");
                exigir(v, vf, ubicacion, btn, "outline", "true");
            }
        }
        Violacion.assertNone("VAR-5.1 — núcleo canónico de atributos del btnDelete "
                + "(oculto sin id/cid, estilo destructivo btn-danger con outline)", v);
    }

    // ---------------------------------------------------------------- VAR-5.2

    // [VAR-5.2] Verificación:
    //   Sujeto: cada `<grid>` y cada `<form>` de clase **referencia** (variante `Ref`).
    //   Condición:
    //     (a) el grid tiene `canNew="false"` y `canViewOnClick="true"` (y no `canEditOnClick`, coherente con `VAR-8.1`);
    //     (b) todos los `<field>` del form tienen `readonly="true"`.
    @Test
    void var5_2_referenciaSoloLectura() {
        List<Violacion> v = new ArrayList<>();
        for (ViewFile vf : ViewFiles.all()) {
            // (a) grids de clase referencia: sin creación y abriendo en solo lectura
            for (Element grid : vf.grids()) {
                String name = attr(grid, "name");
                NombreVista nv = NombreVista.parse(name);
                if (nv == null || nv.clase() != NombreVista.Clase.REFERENCIA) {
                    continue;
                }
                exigir(v, vf, name, grid, "canNew", "false");
                exigir(v, vf, name, grid, "canViewOnClick", "true");
                if (hasAttr(grid, "canEditOnClick")) {
                    v.add(new Violacion(vf.rel(), name,
                            "un grid de referencia no debe llevar canEditOnClick"));
                }
            }
            // (b) forms de clase referencia: todos sus <field> con readonly="true"
            for (Element form : vf.forms()) {
                String name = attr(form, "name");
                NombreVista nv = NombreVista.parse(name);
                if (nv == null || nv.clase() != NombreVista.Clase.REFERENCIA) {
                    continue;
                }
                for (Element field : byTag(form, "field")) {
                    if (!"true".equals(attr(field, "readonly"))) {
                        v.add(new Violacion(vf.rel(), name,
                                "el <field name=\"" + attr(field, "name")
                                        + "\"> debe llevar readonly=\"true\""));
                    }
                }
            }
        }
        Violacion.assertNone("VAR-5.2 — la clase referencia es de solo lectura "
                + "(grid Ref con canNew=false y canViewOnClick=true; fields del form Ref readonly)", v);
    }

    // ---------------------------------------------------------------- helpers

    /** Exige que el atributo esté presente con exactamente ese valor. */
    private static void exigir(List<Violacion> v, ViewFile vf, String ubicacion,
                               Element e, String atributo, String esperado) {
        if (!esperado.equals(attr(e, atributo))) {
            v.add(new Violacion(vf.rel(), ubicacion,
                    "debe llevar " + atributo + "=\"" + esperado + "\"; "
                            + (hasAttr(e, atributo)
                                ? "vale \"" + attr(e, atributo) + "\"" : "está ausente")));
        }
    }

    /** Nombre del form/grid ancestro más cercano (para ubicar elementos sin name propio útil). */
    private static String ubicacionEnFichero(ViewFile vf, Element e) {
        for (org.w3c.dom.Node n = e.getParentNode(); n != null; n = n.getParentNode()) {
            if (n instanceof Element el
                    && (el.getTagName().equals("form") || el.getTagName().equals("grid"))) {
                return attr(el, "name");
            }
        }
        return vf.fileName();
    }
}
