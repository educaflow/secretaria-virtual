package com.educaflow.views.support;

import org.w3c.dom.Element;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Índices globales sobre TODO el conjunto de vistas (para reglas cross-XML) y utilidades de
 * resolución de acciones de botones. Cacheado.
 */
public final class Index {

    /**
     * Acciones globales/predefinidas (no llevan '@'), según el glosario de
     * agent_docs/view-rules.md §«Acciones globales/predefinidas».
     */
    public static final Set<String> PREDEFINIDAS = Set.of(
            "save", "back", "force-back", "delete", "close",
            "save-modal", "delete-modal", "new", "validate",
            "remote-validationSave-action", "remote-validationDelete-action");

    private static Set<String> gruposCache;
    private static Set<String> accionesCache;
    private static Map<String, String> modeloPorVistaCache;
    private static Set<String> formsDeActionViewCache;

    private Index() {}

    /** Nombres de todos los <action-group> del conjunto. */
    public static synchronized Set<String> grupos() {
        if (gruposCache == null) {
            Set<String> s = new HashSet<>();
            for (ViewFile vf : ViewFiles.all()) {
                for (Element ag : vf.byTag("action-group")) {
                    s.add(ViewFiles.attr(ag, "name"));
                }
            }
            gruposCache = s;
        }
        return gruposCache;
    }

    /** Nombres de TODA acción declarada (action-group, action-method, action-record, action-validate,
     *  action-condition, action-script, action-attrs). */
    public static synchronized Set<String> accionesDeclaradas() {
        if (accionesCache == null) {
            Set<String> s = new HashSet<>();
            for (ViewFile vf : ViewFiles.all()) {
                for (String tag : List.of("action-group", "action-method", "action-record",
                        "action-validate", "action-condition", "action-script", "action-attrs")) {
                    for (Element e : vf.byTag(tag)) {
                        s.add(ViewFiles.attr(e, "name"));
                    }
                }
            }
            accionesCache = s;
        }
        return accionesCache;
    }

    /** Nombres de todos los <action-method> y <action-script> (acciones remotas invocables directamente). */
    public static synchronized Set<String> metodosOScripts() {
        Set<String> s = new HashSet<>();
        for (ViewFile vf : ViewFiles.all()) {
            for (String tag : List.of("action-method", "action-script")) {
                for (Element e : vf.byTag(tag)) {
                    s.add(ViewFiles.attr(e, "name"));
                }
            }
        }
        return s;
    }

    /** name de grid/form -> su atributo model. */
    public static synchronized Map<String, String> modeloPorVista() {
        if (modeloPorVistaCache == null) {
            Map<String, String> m = new HashMap<>();
            for (ViewFile vf : ViewFiles.all()) {
                for (String tag : List.of("grid", "form")) {
                    for (Element e : vf.byTag(tag)) {
                        String name = ViewFiles.attr(e, "name");
                        if (!name.isBlank()) {
                            m.put(name, ViewFiles.attr(e, "model"));
                        }
                    }
                }
            }
            modeloPorVistaCache = m;
        }
        return modeloPorVistaCache;
    }

    /** Nombres de forms referenciados por algún <action-view><view type="form" name="..."/>. */
    public static synchronized Set<String> formsAbiertosPorActionView() {
        if (formsDeActionViewCache == null) {
            Set<String> s = new HashSet<>();
            for (ViewFile vf : ViewFiles.all()) {
                for (Element av : vf.actionViews()) {
                    for (Element v : ViewFiles.byTag(av, "view")) {
                        if ("form".equals(ViewFiles.attr(v, "type"))) {
                            s.add(ViewFiles.attr(v, "name"));
                        }
                    }
                }
            }
            formsDeActionViewCache = s;
        }
        return formsDeActionViewCache;
    }

    /** onClick de un botón por nombre dentro de un form (o "" si no está). */
    public static String onClick(Element form, String buttonName) {
        for (Element b : ViewFiles.byTag(form, "button")) {
            if (buttonName.equals(ViewFiles.attr(b, "name"))) {
                return ViewFiles.attr(b, "onClick");
            }
        }
        return "";
    }

    /** Acciones (en orden) del action-group con ese nombre dentro del fichero, o lista vacía. */
    public static List<String> accionesDeGrupo(ViewFile vf, String groupName) {
        return vf.actionGroups().getOrDefault(groupName, List.of());
    }

    /**
     * true si el form es un form de mantenimiento EDITABLE que PERSISTE: tiene un btnSave cuyo
     * action-group ejecuta la acción `save` del framework. Distingue los forms CRUD estándar (que
     * deben seguir el patrón buttons-panel + canBackOnSave) de los forms de solo lectura o de solo
     * acción (visores, listas, tareas de firma, wizards con botones propios), que no lo siguen.
     */
    public static boolean formPersiste(ViewFile vf, Element form) {
        String onClick = onClick(form, "btnSave");
        return !onClick.isBlank() && accionesDeGrupo(vf, onClick).contains("save");
    }
}
