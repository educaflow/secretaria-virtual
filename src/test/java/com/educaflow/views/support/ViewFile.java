package com.educaflow.views.support;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Un fichero de vistas parseado, con helpers para las reglas de agent_docs/view-rules.md.
 */
public final class ViewFile {

    private final Path path;
    private final Document doc;

    public ViewFile(Path path, Document doc) {
        this.path = path;
        this.doc = doc;
    }

    public Document doc() {
        return doc;
    }

    public Path path() {
        return path;
    }

    /** Nombre del fichero, p.ej. "Ciclo.xml". */
    public String fileName() {
        return path.getFileName().toString();
    }

    /** Ruta relativa legible desde src/main/java/com/educaflow, p.ej. "subsystem/sistemaeducativo/views/Ciclo.xml". */
    public String rel() {
        String s = path.toString().replace('\\', '/');
        int i = s.indexOf("/com/educaflow/");
        return i >= 0 ? s.substring(i + "/com/educaflow/".length()) : s;
    }

    /** true si el fichero está bajo subsystem/. */
    public boolean isSubsystem() {
        return rel().startsWith("subsystem/");
    }

    /** true si el fichero está bajo system/. */
    public boolean isSystem() {
        return rel().startsWith("system/");
    }

    /** Nombre del (sub)sistema dueño, p.ej. "sistemaeducativo". */
    public String ownerModule() {
        String[] parts = rel().split("/");
        return parts.length >= 2 ? parts[1] : "";
    }

    public List<Element> byTag(String tag) {
        return ViewFiles.byTag(doc, tag);
    }

    public List<Element> forms() {
        return byTag("form");
    }

    public List<Element> grids() {
        return byTag("grid");
    }

    public List<Element> actionViews() {
        return byTag("action-view");
    }

    /** name -> lista ordenada de los valores de los <action name="..."/> hijos de cada <action-group>. */
    public Map<String, List<String>> actionGroups() {
        Map<String, List<String>> map = new LinkedHashMap<>();
        for (Element ag : byTag("action-group")) {
            List<String> actions = new ArrayList<>();
            for (Element a : ViewFiles.childrenByTag(ag, "action")) {
                actions.add(ViewFiles.attr(a, "name"));
            }
            map.put(ViewFiles.attr(ag, "name"), actions);
        }
        return map;
    }
}
