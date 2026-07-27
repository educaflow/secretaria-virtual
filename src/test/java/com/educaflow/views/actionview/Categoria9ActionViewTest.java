// =====================================================================
// GENERADO por /code-create-view-tests desde agent_docs/view-rules.md
// NO EDITAR A MANO. Para cambiar un test, edita view-rules.md (o corrige
// la traducción en el skill /code-create-view-tests) y vuelve a ejecutarlo.
// =====================================================================
package com.educaflow.views.actionview;

import com.educaflow.views.support.ViewFile;
import com.educaflow.views.support.ViewFiles;
import com.educaflow.views.support.Violacion;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.educaflow.views.support.ViewFiles.attr;
import static com.educaflow.views.support.ViewFiles.childrenByTag;
import static com.educaflow.views.support.ViewFiles.hasAttr;

/**
 * Categoría 9 — {@code <action-view>} (agent_docs/view-rules.md, VAR-9.1 y VAR-9.2).
 *
 * <p>Tests JUnit 5 planos sobre el DOM de los XML de vistas no exentos.
 */
class Categoria9ActionViewTest {

    /**
     * VAR-9.1 — Estructura del {@code <action-view>}:
     * (a) tiene atributo {@code model};
     * (b) si declara {@code <view type="grid">} y {@code <view type="form">}, el grid aparece
     * antes que el form.
     */
    // [VAR-9.1] Verificación:
    //   Sujeto: cada `<action-view>`.
    //   Condición:
    //     (a) tiene atributo `model`;
    //     (b) si declara `<view type="grid">` y `<view type="form">`, el grid aparece antes que el form.
    @Test
    void var9_1_modelPresenteYGridAntesQueForm() {
        List<Violacion> v = new ArrayList<>();
        for (ViewFile vf : ViewFiles.all()) {
            for (Element av : vf.actionViews()) {
                String nombre = attr(av, "name");
                // (a) atributo model presente
                if (!hasAttr(av, "model")) {
                    v.add(new Violacion(vf.rel(), nombre, "no tiene atributo model"));
                }
                // (b) orden grid antes que form entre sus <view>
                List<Element> views = childrenByTag(av, "view");
                int primerGrid = -1;
                int primerForm = -1;
                for (int i = 0; i < views.size(); i++) {
                    String type = attr(views.get(i), "type");
                    if ("grid".equals(type) && primerGrid < 0) {
                        primerGrid = i;
                    }
                    if ("form".equals(type) && primerForm < 0) {
                        primerForm = i;
                    }
                }
                if (primerGrid >= 0 && primerForm >= 0 && primerForm < primerGrid) {
                    v.add(new Violacion(vf.rel(), nombre,
                            "declara <view type=\"form\"> antes que <view type=\"grid\">"
                                    + " (el grid debe declararse primero)"));
                }
            }
        }
        Violacion.assertNone(
                "VAR-9.1 — Todo <action-view> tiene model y, si declara grid y form, el grid va antes",
                v);
    }

    /**
     * VAR-9.2 — {@code view-param} obligatorios:
     * (a) si el action-view abre un form (algún {@code <view type="form">}) ⇒ existe
     * {@code <view-param name="show-toolbar-form" value="false"/>};
     * (b) si su {@code <view type="grid" name="G">} referencia un grid declarado con
     * {@code canEditOnClick="true"} ⇒ existe {@code <view-param name="forceEdit" value="true"/>}.
     */
    // [VAR-9.2] Verificación:
    //   Sujeto: cada `<action-view>`.
    //   Condición:
    //     (a) si abre un form ⇒ presente `<view-param name="show-toolbar-form" value="false"/>`;
    //     (b) si su `<view type="grid">` referencia un grid con `canEditOnClick="true"` ⇒ presente `<view-param name="forceEdit" value="true"/>`.
    @Test
    void var9_2_viewParamsObligatorios() {
        // Índice global: name de cada <grid> declarado -> ¿tiene canEditOnClick="true"?
        Map<String, Boolean> gridEditable = new HashMap<>();
        for (ViewFile vf : ViewFiles.all()) {
            for (Element grid : vf.grids()) {
                gridEditable.put(attr(grid, "name"), "true".equals(attr(grid, "canEditOnClick")));
            }
        }

        List<Violacion> v = new ArrayList<>();
        for (ViewFile vf : ViewFiles.all()) {
            for (Element av : vf.actionViews()) {
                String nombre = attr(av, "name");
                boolean abreForm = false;
                boolean referenciaGridEditable = false;
                for (Element view : childrenByTag(av, "view")) {
                    String type = attr(view, "type");
                    if ("form".equals(type)) {
                        abreForm = true;
                    }
                    if ("grid".equals(type)
                            && Boolean.TRUE.equals(gridEditable.get(attr(view, "name")))) {
                        referenciaGridEditable = true;
                    }
                }
                // (a) abre un form ⇒ show-toolbar-form=false
                if (abreForm && !tieneViewParam(av, "show-toolbar-form", "false")) {
                    v.add(new Violacion(vf.rel(), nombre,
                            "abre un form pero falta <view-param name=\"show-toolbar-form\" value=\"false\"/>"));
                }
                // (b) grid con canEditOnClick="true" ⇒ forceEdit=true
                if (referenciaGridEditable && !tieneViewParam(av, "forceEdit", "true")) {
                    v.add(new Violacion(vf.rel(), nombre,
                            "referencia un grid con canEditOnClick=\"true\" pero falta"
                                    + " <view-param name=\"forceEdit\" value=\"true\"/>"));
                }
            }
        }
        Violacion.assertNone(
                "VAR-9.2 — view-param obligatorios: show-toolbar-form=false si abre form; forceEdit=true si el grid es canEditOnClick",
                v);
    }

    /** true si el action-view tiene un hijo {@code <view-param name="…" value="…"/>} con esos valores. */
    private static boolean tieneViewParam(Element actionView, String name, String value) {
        for (Element vp : childrenByTag(actionView, "view-param")) {
            if (name.equals(attr(vp, "name")) && value.equals(attr(vp, "value"))) {
                return true;
            }
        }
        return false;
    }
}
