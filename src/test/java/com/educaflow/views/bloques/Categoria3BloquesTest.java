// =====================================================================
// GENERADO por /create-view-test desde agent_docs/view-rules.md
// NO EDITAR A MANO. Para cambiar un test, edita view-rules.md (o corrige
// la traducción en el skill /create-view-test) y vuelve a ejecutarlo.
// =====================================================================
package com.educaflow.views.bloques;

import com.educaflow.views.support.NombreVista;
import com.educaflow.views.support.ViewFile;
import com.educaflow.views.support.ViewFiles;
import com.educaflow.views.support.Violacion;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ProcessingInstruction;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Categoría 3 de agent_docs/view-rules.md — Bloques y secciones (Processing Instructions).
 *
 * <p>Cada fichero de vistas se estructura en <b>bloques</b> delimitados por {@code <?sv-view?>}
 * (un bloque va de su {@code <?sv-view?>} al siguiente {@code <?sv-view?>} o al fin del fichero)
 * y, dentro de cada bloque, la zona de acciones se parte en cuatro secciones marcadas con las PI
 * {@code <?sv-primary-actions?>} / {@code <?sv-validations?>} / {@code <?sv-rules?>} /
 * {@code <?sv-remotes?>}.
 *
 * <p>Los elementos de alto nivel cuyo {@code name} NO contiene {@code @} (overrides del framework
 * Axelor, p.ej. {@code user-preferences-form}) quedan fuera: no forman bloque ni exigen PI.
 */
class Categoria3BloquesTest {

    /** Vocabulario cerrado de PI, en el orden canónico dentro de cada bloque. */
    private static final List<String> PI_ORDEN =
            List.of("sv-view", "sv-primary-actions", "sv-validations", "sv-rules", "sv-remotes");
    private static final Set<String> PI_PERMITIDAS = Set.copyOf(PI_ORDEN);

    /** Elementos de alto nivel del glosario (lo que ve el usuario), frente a las acciones. */
    private static final Set<String> ALTO_NIVEL = Set.of("action-view", "grid", "form", "tree", "chart");

    // ------------------------------------------------------------------
    // Modelo de bloques: partición de los hijos directos de <object-views>
    // ------------------------------------------------------------------

    /** Un bloque: la secuencia de nodos (PIs y elementos con '@' en el name) desde su {@code <?sv-view?>}. */
    private record Bloque(List<Node> items) {

        List<ProcessingInstruction> pis() {
            List<ProcessingInstruction> out = new ArrayList<>();
            for (Node n : items) {
                if (n.getNodeType() == Node.PROCESSING_INSTRUCTION_NODE) {
                    out.add((ProcessingInstruction) n);
                }
            }
            return out;
        }

        List<Element> elementos() {
            List<Element> out = new ArrayList<>();
            for (Node n : items) {
                if (n.getNodeType() == Node.ELEMENT_NODE) {
                    out.add((Element) n);
                }
            }
            return out;
        }

        /** Descomposición del contexto del bloque (el primer name descomponible), o null. */
        NombreVista nombre() {
            for (Element e : elementos()) {
                NombreVista nv = NombreVista.parse(ViewFiles.attr(e, "name"));
                if (nv != null) {
                    return nv;
                }
            }
            return null;
        }
    }

    /** Partición del fichero: preámbulo (nodos antes del primer {@code <?sv-view?>}) + bloques. */
    private record Particion(List<Node> preambulo, List<Bloque> bloques) {}

    /**
     * Recorre los hijos directos de {@code <object-views>} y los parte en bloques: cada
     * {@code <?sv-view?>} abre un bloque nuevo. Los elementos cuyo name no contiene {@code @}
     * (overrides Axelor) se descartan: no forman bloque ni exigen PI.
     */
    private static Particion particionar(ViewFile vf) {
        List<Node> preambulo = new ArrayList<>();
        List<Bloque> bloques = new ArrayList<>();
        List<Node> actual = null;
        NodeList nl = vf.doc().getDocumentElement().getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node n = nl.item(i);
            if (n.getNodeType() == Node.PROCESSING_INSTRUCTION_NODE) {
                if ("sv-view".equals(((ProcessingInstruction) n).getTarget())) {
                    if (actual != null) {
                        bloques.add(new Bloque(actual));
                    }
                    actual = new ArrayList<>();
                }
                (actual != null ? actual : preambulo).add(n);
            } else if (n.getNodeType() == Node.ELEMENT_NODE) {
                if (!ViewFiles.attr((Element) n, "name").contains("@")) {
                    continue; // override del framework Axelor: fuera de la estructura de bloques
                }
                (actual != null ? actual : preambulo).add(n);
            }
        }
        if (actual != null) {
            bloques.add(new Bloque(actual));
        }
        return new Particion(preambulo, bloques);
    }

    /** Ubicación legible de un bloque para los mensajes de violación. */
    private static String ubicacion(int idx, Bloque b) {
        NombreVista nv = b.nombre();
        return "bloque " + (idx + 1) + (nv != null ? " (" + nv.contexto() + ")" : "");
    }

    /** Descripción legible de un nodo (PI o elemento) para los mensajes de violación. */
    private static String describe(Node n) {
        if (n.getNodeType() == Node.PROCESSING_INSTRUCTION_NODE) {
            return "<?" + ((ProcessingInstruction) n).getTarget() + "?>";
        }
        return "<" + n.getNodeName() + " name=\"" + ViewFiles.attr((Element) n, "name") + "\">";
    }

    // ------------------------------------------------------------------
    // VAR-3.1
    // ------------------------------------------------------------------

    /**
     * VAR-3.1 — Cada bloque presenta las cinco PI exactamente una vez cada una y en el orden
     * sv-view → sv-primary-actions → sv-validations → sv-rules → sv-remotes. Sin exención:
     * también los bloques Ref y los de solo lectura. Nada (PI de sección o elemento con '@')
     * puede aparecer antes del primer {@code <?sv-view?>}: ese contenido no pertenece a ningún
     * bloque, es decir, forma un "bloque" que no abre con {@code <?sv-view?>}.
     */
    // [VAR-3.1] Verificación:
    //   Sujeto: cada **bloque** de un fichero en alcance.
    //   Condición: el bloque presenta sus cinco PI **exactamente una vez cada una y en este orden**, aunque una sección quede vacía —
    //     `<?sv-view?>` → `<?sv-primary-actions?>` → `<?sv-validations?>` → `<?sv-rules?>` → `<?sv-remotes?>` —
    //     y el bloque siguiente **vuelve a empezar** por `<?sv-view?>` con la misma secuencia.
    //   Aplica a **todo** bloque, sin exención: también los `Ref` y las vistas de utilidad llevan las cinco PI.
    //   Fuera del sujeto: los elementos de alto nivel cuyo `name` **no contiene `@`** (overrides del framework Axelor como `user-preferences-form`) no forman bloque, así que no llevan PI.
    @Test
    void var3_1_cincoPIsUnaVezCadaUnaYEnOrden() {
        List<Violacion> v = new ArrayList<>();
        for (ViewFile vf : ViewFiles.all()) {
            Particion p = particionar(vf);
            for (Node n : p.preambulo()) {
                v.add(new Violacion(vf.rel(), "preámbulo", describe(n)
                        + " aparece antes del primer <?sv-view?>: todo bloque debe abrir con <?sv-view?>"));
            }
            for (int i = 0; i < p.bloques().size(); i++) {
                Bloque b = p.bloques().get(i);
                List<String> targets = b.pis().stream().map(ProcessingInstruction::getTarget).toList();
                if (!targets.equals(PI_ORDEN)) {
                    v.add(new Violacion(vf.rel(), ubicacion(i, b),
                            "las PI del bloque son " + targets + "; se esperaban exactamente " + PI_ORDEN
                                    + " (una vez cada una y en ese orden)"));
                }
            }
        }
        Violacion.assertNone("VAR-3.1 — Las cinco PI, una vez cada una y en orden, en todo bloque", v);
    }

    // ------------------------------------------------------------------
    // VAR-3.2
    // ------------------------------------------------------------------

    /**
     * VAR-3.2 — Entre {@code <?sv-view?>} y {@code <?sv-primary-actions?>} solo hay elementos de
     * alto nivel (action-view/grid/form/tree/chart), ninguna acción, en el orden
     * action-view → grid (opcional) → form/tree/chart. Del action-view: 0 o 1 por bloque, solo en
     * el bloque de clase maestro y, si existe, su name es {@code {contexto}-action}.
     */
    // [VAR-3.2] Verificación:
    //   Sujeto: los elementos entre `<?sv-view?>` y el `<?sv-primary-actions?>` del mismo bloque.
    //   Condición: son **solo** elementos de alto nivel (`action-view`/`grid`/`form`/`tree`/`chart`), ninguna acción, y en el orden
    //     `action-view` → `grid` (opcional) → `form`/`tree`/`chart`.
    //   Del `action-view`:
    //     hay **0 o 1** en el bloque, y solo puede aparecer en el **bloque maestro**; los bloques de detalle y los de referencia no lo llevan;
    //     cuando existe, su `name` es el del bloque más `-action` (`{contexto}-action`).
    @Test
    void var3_2_svViewEncabezaLasVistasDeAltoNivelEnOrden() {
        List<Violacion> v = new ArrayList<>();
        for (ViewFile vf : ViewFiles.all()) {
            Particion p = particionar(vf);
            for (int i = 0; i < p.bloques().size(); i++) {
                Bloque b = p.bloques().get(i);
                String ub = ubicacion(i, b);

                // Zona de vistas: elementos tras el <?sv-view?> inicial y antes de la siguiente PI del bloque.
                List<Element> zona = new ArrayList<>();
                for (int j = 1; j < b.items().size(); j++) {
                    Node n = b.items().get(j);
                    if (n.getNodeType() == Node.PROCESSING_INSTRUCTION_NODE) {
                        break;
                    }
                    zona.add((Element) n);
                }

                int rangoAnterior = -1;
                List<Element> actionViews = new ArrayList<>();
                for (Element e : zona) {
                    String tag = e.getNodeName();
                    if (!ALTO_NIVEL.contains(tag)) {
                        v.add(new Violacion(vf.rel(), ub, describe(e)
                                + " entre <?sv-view?> y <?sv-primary-actions?>: esa zona solo admite elementos"
                                + " de alto nivel (action-view/grid/form/tree/chart), ninguna acción"));
                        continue;
                    }
                    int rango = switch (tag) {
                        case "action-view" -> 0;
                        case "grid" -> 1;
                        default -> 2; // form / tree / chart
                    };
                    if (rango < rangoAnterior) {
                        v.add(new Violacion(vf.rel(), ub, describe(e)
                                + " fuera de orden: la zona de vistas sigue el orden action-view → grid → form/tree/chart"));
                    }
                    rangoAnterior = Math.max(rangoAnterior, rango);
                    if ("action-view".equals(tag)) {
                        actionViews.add(e);
                    }
                }

                if (actionViews.size() > 1) {
                    v.add(new Violacion(vf.rel(), ub,
                            "hay " + actionViews.size() + " <action-view> en el bloque; se admiten 0 o 1"));
                }
                if (!actionViews.isEmpty()) {
                    NombreVista nv = b.nombre();
                    if (nv != null) {
                        if (nv.clase() != NombreVista.Clase.MAESTRO) {
                            v.add(new Violacion(vf.rel(), ub, "el bloque es de clase " + nv.clase()
                                    + " y declara un <action-view>: solo el bloque de clase maestro puede llevarlo"));
                        }
                        String esperado = nv.contexto() + "-action";
                        for (Element av : actionViews) {
                            String name = ViewFiles.attr(av, "name");
                            if (!esperado.equals(name)) {
                                v.add(new Violacion(vf.rel(), ub, "<action-view name=\"" + name
                                        + "\">: su name debe ser el del bloque más -action (" + esperado + ")"));
                            }
                        }
                    }
                }
            }
        }
        Violacion.assertNone("VAR-3.2 — <?sv-view?> encabeza las vistas de alto nivel del bloque", v);
    }

    // ------------------------------------------------------------------
    // VAR-3.3
    // ------------------------------------------------------------------

    /**
     * VAR-3.3 — Cada sección contiene solo las acciones de su rol, según la tabla «Rol de una
     * acción» del glosario. El rol lo declara el arranque de la descripcion del name, NO el tipo
     * de elemento: un action-group {@code Local-…} va bajo sv-validations; un action-group
     * {@code btn…}/{@code on…} bajo sv-primary-actions; {@code set-…} bajo sv-rules;
     * {@code Remote-…} bajo sv-remotes.
     */
    // [VAR-3.3] Verificación:
    //   Sujeto: los elementos entre la PI de cada sección y la siguiente PI del bloque (en la última sección, hasta el siguiente `<?sv-view?>` o el fin del fichero).
    //   Condición: cada sección contiene 0..n acciones, todas del **rol** de esa sección (tabla «Rol de una acción» del glosario).
    @Test
    void var3_3_cadaSeccionSoloAccionesDeSuRol() {
        List<Violacion> v = new ArrayList<>();
        for (ViewFile vf : ViewFiles.all()) {
            Particion p = particionar(vf);
            for (int i = 0; i < p.bloques().size(); i++) {
                Bloque b = p.bloques().get(i);
                String ub = ubicacion(i, b);
                String seccion = null;
                for (Node n : b.items()) {
                    if (n.getNodeType() == Node.PROCESSING_INSTRUCTION_NODE) {
                        seccion = ((ProcessingInstruction) n).getTarget();
                        continue;
                    }
                    if (seccion == null || "sv-view".equals(seccion)) {
                        continue; // la zona de vistas la verifica VAR-3.2
                    }
                    Element e = (Element) n;
                    if (ALTO_NIVEL.contains(e.getNodeName())) {
                        v.add(new Violacion(vf.rel(), ub, describe(e) + " bajo <?" + seccion
                                + "?>: las secciones de acción solo contienen acciones"));
                        continue;
                    }
                    NombreVista nv = NombreVista.parse(ViewFiles.attr(e, "name"));
                    String esperada = nv == null ? null : seccionDeRol(nv.descripcion());
                    if (esperada == null) {
                        v.add(new Violacion(vf.rel(), ub, describe(e)
                                + ": su descripcion no declara ningún rol (btn…/on…/Local-/set-/Remote-),"
                                + " así que no se puede ubicar en ninguna sección"));
                    } else if (!esperada.equals(seccion)) {
                        v.add(new Violacion(vf.rel(), ub, describe(e) + " está bajo <?" + seccion
                                + "?> pero el rol que declara su descripcion corresponde a <?" + esperada + "?>"));
                    }
                }
            }
        }
        Violacion.assertNone("VAR-3.3 — Cada sección contiene solo las acciones de su rol", v);
    }

    /** Sección (PI) que corresponde al rol declarado por el arranque de la descripcion, o null. */
    private static String seccionDeRol(String descripcion) {
        if (descripcion.startsWith("Local-")) {
            return "sv-validations";
        }
        if (descripcion.startsWith("Remote-")) {
            return "sv-remotes";
        }
        if (descripcion.startsWith("set-")) {
            return "sv-rules";
        }
        if (descripcion.matches("^btn.*") || descripcion.matches("^on[A-Z].*")) {
            return "sv-primary-actions";
        }
        return null;
    }

    // ------------------------------------------------------------------
    // VAR-3.4
    // ------------------------------------------------------------------

    /**
     * VAR-3.4 — (a) el target de toda PI del documento pertenece al vocabulario cerrado
     * sv-view/sv-primary-actions/sv-validations/sv-rules/sv-remotes; (b) no hay ningún nodo
     * comentario que haga de banner de bloque/sección (corridas de 3+ asteriscos, o el texto
     * «: Vistas»/«: Acciones»).
     */
    // [VAR-3.4] Verificación:
    //   Sujeto: cada PI y cada nodo comentario de un fichero en alcance.
    //   Condición:
    //     (a) el `target` de toda PI es uno de `sv-view`, `sv-primary-actions`, `sv-validations`, `sv-rules`, `sv-remotes` (no hay otras `sv-*` ni PIs de target desconocido);
    //     (b) no hay ningún nodo comentario que haga de **banner de bloque/sección** (corridas de asteriscos, o el texto `: Vistas`/`: Acciones`).
    @Test
    void var3_4_vocabularioPICerradoYSinBanners() {
        List<Violacion> v = new ArrayList<>();
        for (ViewFile vf : ViewFiles.all()) {
            List<ProcessingInstruction> pis = new ArrayList<>();
            List<Node> comentarios = new ArrayList<>();
            recolectar(vf.doc(), pis, comentarios);
            for (ProcessingInstruction pi : pis) {
                if (!PI_PERMITIDAS.contains(pi.getTarget())) {
                    v.add(new Violacion(vf.rel(), "<?" + pi.getTarget() + "?>",
                            "target de PI fuera del vocabulario cerrado " + PI_ORDEN));
                }
            }
            for (Node c : comentarios) {
                String texto = c.getNodeValue() == null ? "" : c.getNodeValue();
                if (texto.matches("(?s).*\\*{3,}.*") || texto.contains(": Vistas") || texto.contains(": Acciones")) {
                    v.add(new Violacion(vf.rel(), "comentario",
                            "comentario-banner de bloque/sección (la estructura la marcan solo las PI): \""
                                    + resumen(texto) + "\""));
                }
            }
        }
        Violacion.assertNone("VAR-3.4 — Vocabulario de PI cerrado; sin banners de comentarios", v);
    }

    /** Recolecta recursivamente todas las PI y todos los comentarios del documento entero. */
    private static void recolectar(Node n, List<ProcessingInstruction> pis, List<Node> comentarios) {
        if (n.getNodeType() == Node.PROCESSING_INSTRUCTION_NODE) {
            pis.add((ProcessingInstruction) n);
        } else if (n.getNodeType() == Node.COMMENT_NODE) {
            comentarios.add(n);
        }
        for (Node h = n.getFirstChild(); h != null; h = h.getNextSibling()) {
            recolectar(h, pis, comentarios);
        }
    }

    private static String resumen(String texto) {
        String t = texto.trim().replaceAll("\\s+", " ");
        return t.length() <= 80 ? t : t.substring(0, 77) + "…";
    }

    // ------------------------------------------------------------------
    // VAR-3.5
    // ------------------------------------------------------------------

    /**
     * VAR-3.5 — Todos los elementos con name con '@' de un bloque comparten el mismo contexto
     * (todo lo anterior al primer '-'). Las referencias {@code <action name="…">} DENTRO de un
     * action-group no son sujeto: no son hijos directos de {@code <object-views>} y la partición
     * en bloques no las recorre.
     */
    // [VAR-3.5] Verificación:
    //   Sujeto: los elementos con `name` que contiene `@` de cada bloque (entre su `<?sv-view?>` y el siguiente).
    //   Condición: todos comparten el mismo contexto (`{marcadorCapa}{Módulo}.{Variante}@{ruta de entidad}`, es decir todo lo anterior al primer `-`).
    //   Exención: las referencias `<action name="…">` **dentro** de un `action-group` no son sujetos (pueden apuntar a globales o a acciones del bloque).
    @Test
    void var3_5_contextoUniformeDentroDeCadaBloque() {
        List<Violacion> v = new ArrayList<>();
        for (ViewFile vf : ViewFiles.all()) {
            Particion p = particionar(vf);
            for (int i = 0; i < p.bloques().size(); i++) {
                Bloque b = p.bloques().get(i);
                String contextoBloque = null;
                for (Element e : b.elementos()) {
                    String name = ViewFiles.attr(e, "name"); // siempre contiene '@' (la partición excluye el resto)
                    String ctx = contextoTextual(name);
                    if (contextoBloque == null) {
                        contextoBloque = ctx;
                    } else if (!contextoBloque.equals(ctx)) {
                        v.add(new Violacion(vf.rel(), "bloque " + (i + 1) + " (" + contextoBloque + ")",
                                describe(e) + " tiene contexto " + ctx + ", distinto del contexto del bloque"));
                    }
                }
            }
        }
        Violacion.assertNone("VAR-3.5 — Contexto uniforme dentro de cada bloque", v);
    }

    /** Contexto del glosario: todo lo anterior al primer '-' del name. */
    private static String contextoTextual(String name) {
        int i = name.indexOf('-');
        return i < 0 ? name : name.substring(0, i);
    }

    // ------------------------------------------------------------------
    // VAR-3.6
    // ------------------------------------------------------------------

    /**
     * VAR-3.6 — Los bloques van de maestro a detalle: (a) el primer bloque es de nivel 1 y su
     * {@code Variante@Entidad} casa con el nombre del fichero {@code {Variante}-{Entidad}.xml};
     * (b) el nivel (nº de segmentos de la ruta de entidad) es monótono no decreciente;
     * (c) la ruta de todo bloque de nivel ≥2, sin su último segmento, es la ruta de un bloque
     * anterior del fichero; (d) no hay dos bloques con el mismo contexto.
     */
    // [VAR-3.6] Verificación:
    //   Sujeto: la secuencia de bloques de cada fichero en alcance, con sus rutas de entidad (nº de segmentos = nivel).
    //   Condición:
    //     (a) el primer bloque es el **maestro**: nivel 1 y su `Variante@Entidad` casa con el nombre del fichero (`VAR-1.2`);
    //     (b) recorriendo los bloques en orden, el nivel es monótono no decreciente (igual profundidad permitida para detalles hermanos);
    //     (c) la ruta de entidad de todo bloque de nivel ≥2, quitándole el último segmento, es la ruta de un bloque anterior del fichero;
    //     (d) no hay dos bloques con el mismo contexto.
    @Test
    void var3_6_bloquesDeMaestroADetalle() {
        List<Violacion> v = new ArrayList<>();
        for (ViewFile vf : ViewFiles.all()) {
            Particion p = particionar(vf);
            // Contextos descomponibles de los bloques, en orden de aparición.
            List<NombreVista> ctxs = new ArrayList<>();
            for (Bloque b : p.bloques()) {
                NombreVista nv = b.nombre();
                if (nv != null) {
                    ctxs.add(nv);
                }
            }
            if (ctxs.isEmpty()) {
                continue; // fichero sin bloques (p.ej. solo overrides Axelor sin '@')
            }

            // (a) el primer bloque es el maestro de nivel 1 y casa con el nombre del fichero
            NombreVista primero = ctxs.get(0);
            String base = vf.fileName().replaceFirst("\\.xml$", "");
            int guion = base.indexOf('-');
            String varianteFichero = guion < 0 ? "" : base.substring(0, guion);
            String entidadFichero = guion < 0 ? "" : base.substring(guion + 1);
            if (primero.rutaEntidad().size() != 1) {
                v.add(new Violacion(vf.rel(), "bloque 1 (" + primero.contexto() + ")",
                        "el primer bloque es de nivel " + primero.rutaEntidad().size()
                                + "; el fichero debe abrir con el bloque maestro (nivel 1)"));
            }
            if (!primero.variante().equals(varianteFichero)
                    || !primero.rutaEntidad().get(0).equals(entidadFichero)) {
                v.add(new Violacion(vf.rel(), "bloque 1 (" + primero.contexto() + ")",
                        "el primer bloque es " + primero.variante() + "@" + String.join(".", primero.rutaEntidad())
                                + " y no casa con el nombre del fichero {Variante}-{Entidad}.xml (" + base + ")"));
            }

            // (b) + (c) + (d)
            int nivelAnterior = 0;
            List<String> rutasAnteriores = new ArrayList<>();
            Set<String> contextos = new HashSet<>();
            for (NombreVista nv : ctxs) {
                String ub = "bloque (" + nv.contexto() + ")";
                int nivel = nv.rutaEntidad().size();
                String ruta = String.join(".", nv.rutaEntidad());
                if (nivel < nivelAnterior) {
                    v.add(new Violacion(vf.rel(), ub, "el nivel baja de " + nivelAnterior + " a " + nivel
                            + ": la secuencia de bloques debe ser monótona no decreciente (de maestro a detalle)"));
                }
                if (nivel >= 2) {
                    String rutaPadre = ruta.substring(0, ruta.lastIndexOf('.'));
                    if (!rutasAnteriores.contains(rutaPadre)) {
                        v.add(new Violacion(vf.rel(), ub, "no existe antes en el fichero ningún bloque con ruta "
                                + rutaPadre + " del que cuelgue este detalle"));
                    }
                }
                if (!contextos.add(nv.contexto())) {
                    v.add(new Violacion(vf.rel(), ub, "contexto de bloque duplicado en el fichero"));
                }
                rutasAnteriores.add(ruta);
                nivelAnterior = nivel;
            }
        }
        Violacion.assertNone("VAR-3.6 — Los bloques van de maestro a detalle", v);
    }
}
