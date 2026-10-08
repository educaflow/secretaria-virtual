// =====================================================================
// GENERADO por /developer-create-view-tests desde agent_docs/view-rules.md
// NO EDITAR A MANO. Para cambiar un test, edita view-rules.md (o corrige
// la traducción en el skill /developer-create-view-tests) y vuelve a ejecutarlo.
// =====================================================================
package com.educaflow.views.forms;

import com.educaflow.views.support.Index;
import com.educaflow.views.support.NombreVista;
import com.educaflow.views.support.ViewFile;
import com.educaflow.views.support.ViewFiles;
import com.educaflow.views.support.Violacion;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

    // ---------------------------------------------------------------- VAR-6.6

    // [VAR-6.6] Verificación:
    //   Sujeto: cada `<field>` descendiente de un `<form>`, lleve o no `readonly`.
    //     Los `<field>` de un `<grid>` no son sujeto.
    //   Condición: su atributo `widget` no vale `SwitchSelect` (comparado sin distinguir mayúsculas ni guiones: tampoco `switch-select`).
    @Test
    void var6_6_ningunCampoDeFormUsaSwitchSelect() {
        List<Violacion> v = new ArrayList<>();
        for (ViewFile vf : ViewFiles.all()) {
            for (Element form : vf.forms()) {
                for (Element field : byTag(form, "field")) {
                    String widget = attr(field, "widget");
                    if ("switchselect".equals(widget.replace("-", "").toLowerCase(Locale.ROOT))) {
                        v.add(new Violacion(vf.rel(), attr(form, "name"),
                                "el <field name=\"" + attr(field, "name") + "\"> usa widget=\"" + widget
                                        + "\"; SwitchSelect no se usa en los forms"));
                    }
                }
            }
        }
        Violacion.assertNone("VAR-6.6 — ningún <field> de un <form> usa widget=\"SwitchSelect\"", v);
    }

    // ---------------------------------------------------------------- VAR-6.7

    // [VAR-6.7] Verificación:
    //   Sujeto: cada `<field>` descendiente de un `<form>` que no es de solo lectura, se pinte o no (`hidden`, `showIf`), cuyo `name` es el de un campo de tipo enumerado de su entidad.
    //     Un `<field>` es de solo lectura si lleva `readonly="true"` o lo lleva alguno de sus ancestros hasta el `<form>` inclusive (un `<panel readonly="true">`, por ejemplo).
    //     La entidad de un `<field>` es la del form, salvo que esté dentro de un `<editor>`:
    //       entonces es la entidad a la que apunta el `ref` del campo relacional dueño de ese `<editor>` (el `<field>` padre del `<editor>`), buscado en la entidad de ese dueño.
    //     La entidad del form es la `<entity>` de los dominios XML (`**/domains/*.xml`) cuyo `package` de `<module>` más su `name` == el `model` del form.
    //     Un campo es de tipo enumerado si su entidad, o una entidad de la que hereda (`extends`), lo declara con un hijo `<enum name="…">`.
    //     No son sujeto: los `<field>` de un `<grid>` ni los `<field>` hijos de un `<panel-related>` (son columnas de su rejilla, no campos del form).
    //   Condición: lleva `widget="RadioSelect"`.
    @Test
    void var6_7_camposEnumeradosDeFormUsanRadioSelect() {
        List<Violacion> v = new ArrayList<>();
        for (ViewFile vf : ViewFiles.all()) {
            for (Element form : vf.forms()) {
                for (Element field : camposEnumeradosDelForm(form).keySet()) {
                    if (esDeSoloLectura(form, field)) {
                        continue; // de solo lectura: no es sujeto
                    }
                    if (!"RadioSelect".equals(attr(field, "widget"))) {
                        v.add(new Violacion(vf.rel(), attr(form, "name"),
                                "el <field name=\"" + attr(field, "name") + "\"> es de tipo enumerado y "
                                        + (hasAttr(field, "widget")
                                                ? "lleva widget=\"" + attr(field, "widget") + "\""
                                                : "no lleva widget")
                                        + "; debe llevar widget=\"RadioSelect\""));
                    }
                }
            }
        }
        Violacion.assertNone("VAR-6.7 — todo <field> editable de un <form> cuyo tipo es un enumerado lleva "
                + "widget=\"RadioSelect\"", v);
    }

    /**
     * Un {@code <field>} es de solo lectura si lleva {@code readonly="true"} él o alguno de sus
     * ancestros hasta el {@code <form>} inclusive.
     */
    private static boolean esDeSoloLectura(Element form, Element field) {
        for (Node n = field; n instanceof Element e; n = n.getParentNode()) {
            if ("true".equals(attr(e, "readonly"))) {
                return true;
            }
            if (e == form) {
                break;
            }
        }
        return false;
    }

    /**
     * Los {@code <field>} del form cuyo name es un campo de tipo enumerado de su entidad, sean o
     * no de solo lectura, cada uno con el número de valores de su enumerado; sin los hijos de
     * {@code <panel-related>}. VAR-6.7 descarta los de solo lectura; VAR-6.8 los incluye.
     */
    private static Map<Element, Integer> camposEnumeradosDelForm(Element form) {
        Map<Element, Integer> campos = new LinkedHashMap<>();
        for (Element field : byTag(form, "field")) {
            // los <field> hijos de un <panel-related> son columnas de su rejilla
            if ("panel-related".equals(field.getParentNode().getNodeName())) {
                continue;
            }
            String entidad = entidadDelCampo(form, field);
            if (entidad == null) {
                continue;
            }
            Integer valores = ViewFiles.camposEnumerados(entidad).get(attr(field, "name"));
            if (valores != null) {
                campos.put(field, valores);
            }
        }
        return campos;
    }

    /**
     * La entidad de un {@code <field>}: la del form, salvo dentro de un {@code <editor>}, donde es la
     * entidad a la que apunta el campo relacional dueño del editor. {@code null} si no se puede
     * resolver contra los dominios.
     */
    private static String entidadDelCampo(Element form, Element field) {
        for (Node n = field.getParentNode(); n instanceof Element e; n = n.getParentNode()) {
            if ("editor".equals(e.getNodeName()) && e.getParentNode() instanceof Element duenyo) {
                String entidadDelDuenyo = entidadDelCampo(form, duenyo);
                return entidadDelDuenyo == null ? null
                        : ViewFiles.entidadRelacionada(entidadDelDuenyo, attr(duenyo, "name"));
            }
        }
        return attr(form, "model");
    }

    // ---------------------------------------------------------------- VAR-6.8

    // [VAR-6.8] Verificación:
    //   Sujeto: cada `<field>` que es sujeto de `VAR-6.7`, o lo sería de no ser de solo lectura, y lleva `widget="RadioSelect"`.
    //   Condición, con `{n}` = el número de valores del enumerado del campo:
    //     lleva el atributo `x-direction`;
    //     si `{n}` <= 4 vale `horizontal`, y si `{n}` >= 5 vale `vertical`.
    //   `{n}` es el número de hijos `<item>` del `<enum>` de nivel superior (hijo de `<domain-models>`) de los dominios XML (`**/domains/*.xml`) al que apunta el `ref` del `<enum name="…">` del campo:
    //     un `ref` con punto es el nombre completo del enumerado (`package` de `<module>` más `name`);
    //     un `ref` sin punto es un enumerado del mismo `package` que la entidad que declara el campo.
    @Test
    void var6_8_orientacionDelRadioSelectSegunNumeroDeValores() {
        List<Violacion> v = new ArrayList<>();
        for (ViewFile vf : ViewFiles.all()) {
            for (Element form : vf.forms()) {
                for (Map.Entry<Element, Integer> sujeto : camposEnumeradosDelForm(form).entrySet()) {
                    Element field = sujeto.getKey();
                    if (!"RadioSelect".equals(attr(field, "widget"))) {
                        continue; // sujeto: solo los que llevan RadioSelect
                    }
                    String campo = attr(field, "name");
                    int n = sujeto.getValue();
                    if (n < 0) {
                        v.add(new Violacion(vf.rel(), attr(form, "name"),
                                "el ref del enumerado del campo \"" + campo
                                        + "\" no apunta a ningún <enum> de nivel superior de los dominios"));
                        continue;
                    }
                    String esperada = n <= 4 ? "horizontal" : "vertical";
                    if (!hasAttr(field, "x-direction")) {
                        v.add(new Violacion(vf.rel(), attr(form, "name"),
                                "el <field name=\"" + campo + "\"> no lleva x-direction; su enumerado tiene "
                                        + n + " valores: debe llevar x-direction=\"" + esperada + "\""));
                    } else if (!esperada.equals(attr(field, "x-direction"))) {
                        v.add(new Violacion(vf.rel(), attr(form, "name"),
                                "el <field name=\"" + campo + "\"> lleva x-direction=\""
                                        + attr(field, "x-direction") + "\"; su enumerado tiene " + n
                                        + " valores: debe llevar x-direction=\"" + esperada + "\""));
                    }
                }
            }
        }
        Violacion.assertNone("VAR-6.8 — el RadioSelect de un enumerado lleva x-direction: horizontal "
                + "hasta 4 valores, vertical con 5 o más", v);
    }
}
