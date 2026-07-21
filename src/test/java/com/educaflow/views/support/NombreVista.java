package com.educaflow.views.support;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Descomposición del {@code name} de una vista o acción según el glosario de
 * agent_docs/view-rules.md: {@code {contexto}-[{descripcion}-]{tipo}}, con
 * {@code contexto = {marcadorCapa}{Modulo}.{Variante}@{Entidad}[.{EntidadDetalle}…]}.
 *
 * <p>De la descomposición se deduce mecánicamente la <b>clase de bloque</b> (glosario):
 * referencia (variante {@code Ref}), detalle (ruta de entidad de ≥2 segmentos) o maestro.
 */
public record NombreVista(String name, String marcadorCapa, String modulo, String variante,
                          List<String> rutaEntidad, String descripcion, String tipo) {

    /** Clase de bloque del glosario. */
    public enum Clase { MAESTRO, DETALLE, REFERENCIA }

    private static final Pattern PREFIJO =
            Pattern.compile("^(subsys|sys)([A-Z][A-Za-z0-9]*)\\.([A-Z][A-Za-z0-9]*)$");

    /**
     * Parsea el name; {@code null} si no sigue el patrón {@code {marcadorCapa}{Modulo}.{Variante}@…}
     * (acciones globales/predefinidas sin '@', nombres Axelor adaptados…).
     */
    public static NombreVista parse(String name) {
        int at = name.indexOf('@');
        if (at < 0) {
            return null;
        }
        Matcher m = PREFIJO.matcher(name.substring(0, at));
        if (!m.matches()) {
            return null;
        }
        String resto = name.substring(at + 1);
        int guion = resto.indexOf('-');
        String ruta = guion < 0 ? resto : resto.substring(0, guion);
        String cola = guion < 0 ? "" : resto.substring(guion + 1);
        int ultimoGuion = cola.lastIndexOf('-');
        String tipo = ultimoGuion < 0 ? cola : cola.substring(ultimoGuion + 1);
        String descripcion = ultimoGuion < 0 ? "" : cola.substring(0, ultimoGuion);
        return new NombreVista(name, m.group(1), m.group(2), m.group(3),
                List.of(ruta.split("\\.")), descripcion, tipo);
    }

    /** El contexto: todo lo anterior al primer '-' que sigue a la ruta de entidad. */
    public String contexto() {
        return marcadorCapa + modulo + "." + variante + "@" + String.join(".", rutaEntidad);
    }

    /** Clase de bloque, deducida mecánicamente del contexto (glosario «Clase de bloque»). */
    public Clase clase() {
        if ("Ref".equals(variante)) {
            return Clase.REFERENCIA;
        }
        return rutaEntidad.size() >= 2 ? Clase.DETALLE : Clase.MAESTRO;
    }

    /** Último segmento de la ruta de entidad (la entidad concreta del bloque). */
    public String entidad() {
        return rutaEntidad.get(rutaEntidad.size() - 1);
    }
}
