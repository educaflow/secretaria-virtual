// =====================================================================
// GENERADO por /code-create-view-tests desde agent_docs/view-rules.md
// NO EDITAR A MANO. Para cambiar un test, edita view-rules.md (o corrige
// la traducción en el skill /code-create-view-tests) y vuelve a ejecutarlo.
// =====================================================================
package com.educaflow.views.grids;

import com.educaflow.views.support.ViewFile;
import com.educaflow.views.support.ViewFiles;
import com.educaflow.views.support.Violacion;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;

import static com.educaflow.views.support.ViewFiles.attr;
import static com.educaflow.views.support.ViewFiles.hasAttr;

/**
 * Categoría 8 — Grids (agent_docs/view-rules.md, VAR-8.1 a VAR-8.3).
 *
 * <p>Reglas estructurales de los {@code <grid>} (los atributos canónicos los verifica la
 * Categoría 5). Tests JUnit 5 planos sobre el DOM de los XML de vistas no exentos.
 */
class Categoria8GridsTest {

    /**
     * VAR-8.1 — Comportamiento de clic único.
     * Cada {@code <grid>} tiene EXACTAMENTE uno de {@code canEditOnClick="true"} /
     * {@code canViewOnClick="true"}: nunca ambos, nunca ninguno.
     */
    // [VAR-8.1] Verificación:
    //   Sujeto: cada `<grid>`.
    //   Condición: tiene **exactamente uno** de `canEditOnClick="true"` / `canViewOnClick="true"` (nunca ambos, nunca ninguno).
    //   (Que en la clase referencia el presente sea `canViewOnClick="true"` lo fija `VAR-5.2`.)
    @Test
    void var8_1_comportamientoDeClicUnico() {
        List<Violacion> v = new ArrayList<>();
        for (ViewFile vf : ViewFiles.all()) {
            for (Element grid : vf.grids()) {
                boolean edit = "true".equals(attr(grid, "canEditOnClick"));
                boolean view = "true".equals(attr(grid, "canViewOnClick"));
                if (edit && view) {
                    v.add(new Violacion(vf.rel(), attr(grid, "name"),
                            "tiene canEditOnClick=\"true\" Y canViewOnClick=\"true\" a la vez"
                                    + " (comportamientos de clic contradictorios)"));
                } else if (!edit && !view) {
                    v.add(new Violacion(vf.rel(), attr(grid, "name"),
                            "no tiene ni canEditOnClick=\"true\" ni canViewOnClick=\"true\""
                                    + " (fila muerta: el clic no abre nada)"));
                }
            }
        }
        Violacion.assertNone(
                "VAR-8.1 — Cada <grid> debe tener exactamente uno de canEditOnClick=\"true\" / canViewOnClick=\"true\"",
                v);
    }

    /**
     * VAR-8.2 — Coherencia {@code canNew}/{@code newButtonTitle} en grids y panel-related.
     * «Puede crear» = {@code canNew="true"}, o {@code canNew} AUSENTE en un {@code <panel-related>}
     * (su defecto es crear); en un {@code <grid>}, {@code canNew} ausente = NO crear.
     * (a) puede crear ⇒ {@code newButtonTitle} presente y no vacío;
     * (b) no puede crear ⇒ {@code newButtonTitle} ausente o vacío.
     */
    // [VAR-8.2] Verificación:
    //   Sujeto: cada `<grid>` y cada `<panel-related>`.
    //   Condición, donde «puede crear» = `canNew="true"`, o `canNew` ausente en un `<panel-related>` (su defecto es crear; en un `<grid>` ausente = no crear):
    //     (a) puede crear ⇒ `newButtonTitle` presente y no vacío;
    //     (b) no puede crear ⇒ `newButtonTitle` ausente o vacío.
    //   (Que un `Ref@…-grid` no pueda crear lo fija `VAR-5.2`.)
    @Test
    void var8_2_coherenciaCanNewNewButtonTitle() {
        List<Violacion> v = new ArrayList<>();
        for (ViewFile vf : ViewFiles.all()) {
            for (Element grid : vf.grids()) {
                comprobarCoherenciaNuevo(v, vf, grid, "true".equals(attr(grid, "canNew")));
            }
            for (Element pr : vf.byTag("panel-related")) {
                boolean puedeCrear = hasAttr(pr, "canNew")
                        ? "true".equals(attr(pr, "canNew"))
                        : true; // en un panel-related el defecto de canNew ausente es crear
                comprobarCoherenciaNuevo(v, vf, pr, puedeCrear);
            }
        }
        Violacion.assertNone(
                "VAR-8.2 — Si se puede crear, newButtonTitle presente y no vacío; si no, ausente o vacío",
                v);
    }

    /** Aplica las cláusulas (a)/(b) de VAR-8.2 a un grid o panel-related. */
    private static void comprobarCoherenciaNuevo(List<Violacion> v, ViewFile vf, Element e, boolean puedeCrear) {
        boolean tituloPresenteNoVacio = hasAttr(e, "newButtonTitle") && !attr(e, "newButtonTitle").isEmpty();
        String ubicacion = e.getTagName() + " " + attr(e, "name");
        if (puedeCrear && !tituloPresenteNoVacio) {
            v.add(new Violacion(vf.rel(), ubicacion,
                    "puede crear registros pero no tiene newButtonTitle (o está vacío)"));
        } else if (!puedeCrear && tituloPresenteNoVacio) {
            v.add(new Violacion(vf.rel(), ubicacion,
                    "no puede crear registros pero tiene newButtonTitle=\"" + attr(e, "newButtonTitle")
                            + "\" (título huérfano)"));
        }
    }

    /**
     * VAR-8.3 — {@code <help>} como primer hijo del grid.
     * En cada {@code <grid>} con hijo {@code <help>}, ese {@code <help>} debe ser el PRIMER hijo
     * elemento del grid; y ningún {@code <grid>} tiene atributo {@code help}.
     */
    // [VAR-8.3] Verificación:
    //   Sujeto: cada `<grid>` con hijo `<help>`.
    //   Condición: el `<help>` es el **primer** hijo del `<grid>`; no existe atributo `help` en el `<grid>`.
    @Test
    void var8_3_helpPrimerHijoDelGrid() {
        List<Violacion> v = new ArrayList<>();
        for (ViewFile vf : ViewFiles.all()) {
            for (Element grid : vf.grids()) {
                if (hasAttr(grid, "help")) {
                    v.add(new Violacion(vf.rel(), attr(grid, "name"),
                            "tiene atributo help=\"…\" (la ayuda va como etiqueta hija <help>, no como atributo)"));
                }
                if (!ViewFiles.childrenByTag(grid, "help").isEmpty()) {
                    Element primero = primerHijoElemento(grid);
                    if (primero == null || !"help".equals(primero.getTagName())) {
                        v.add(new Violacion(vf.rel(), attr(grid, "name"),
                                "tiene un <help> pero no es el primer hijo elemento del <grid>"
                                        + " (el primero es <" + (primero == null ? "?" : primero.getTagName()) + ">)"));
                    }
                }
            }
        }
        Violacion.assertNone(
                "VAR-8.3 — El <help> de un <grid> debe ser su primer hijo elemento y no existir como atributo",
                v);
    }

    /** Primer hijo de tipo elemento de un nodo, o null si no tiene. */
    private static Element primerHijoElemento(Element parent) {
        NodeList nl = parent.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            if (nl.item(i).getNodeType() == Node.ELEMENT_NODE) {
                return (Element) nl.item(i);
            }
        }
        return null;
    }
}
