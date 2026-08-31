package com.educaflow.tiposexpedientes.support;

import com.educaflow.common.buildtools.files.tipoexpediente.Fase;
import com.educaflow.common.buildtools.files.tipoexpediente.State;
import com.educaflow.common.buildtools.files.tipoexpediente.TipoExpedienteInstanceFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lectura del {@code estados.puml} de un tipo de expediente: los estados que el diagrama nombra.
 *
 * <p>Un estado se nombra en el diagrama por su <b>alias</b> {@code <FASE>_<ESTADO>}, porque en
 * PlantUML el identificador de un estado es global y el nombre de un estado del XML solo es único
 * dentro de su fase ({@code k-tipo-expediente} §2.4). Ese alias es lo que se compara con el
 * {@code TipoExpedienteInstance.xml}.
 *
 * <p>Se recogen las dos formas de nombrar un estado, porque las dos lo dibujan:
 *
 * <ul>
 *   <li><b>Declaración</b>: {@code state "ENTRADA_DATOS" as RECEPCION_ENTRADA_DATOS}.</li>
 *   <li><b>Uso</b>: el identificador en un extremo de una transición ({@code A --> B : EVENTO}) o en
 *       una anotación ({@code TRAMITACION_ACEPTADO : closed}). En PlantUML un identificador usado
 *       sin declarar <b>no es un error</b>: crea un nodo nuevo en silencio, que es exactamente como
 *       un typo en el destino de una transición se convierte en un estado fantasma del dibujo.</li>
 * </ul>
 *
 * <p>Se ignoran los comentarios ({@code ' …} de línea y bloques {@code /' … '/}), las directivas
 * ({@code @startuml}, {@code skinparam}, {@code title}, {@code note}…), el pseudo-estado
 * {@code [*]} —que no es un estado sino el marcador de inicio, y como destino significa borrado
 * físico— y los <b>estados compuestos</b> ({@code state RECEPCION \{}), que representan la fase y no
 * un estado.
 */
public final class DiagramaDeEstados {

    /** El nombre del diagrama, en la raíz de la carpeta de versión, junto al XML maestro. */
    public static final String NOMBRE_FICHERO = "estados.puml";

    /** {@code state "NOMBRE" as ALIAS} o {@code state NOMBRE as ALIAS}: el estado es el alias. */
    private static final Pattern DECLARACION_CON_ALIAS =
            Pattern.compile("^state\\s+(?:\"[^\"]*\"|\\w+)\\s+as\\s+(\\w+)\\s*$");

    /** {@code state NOMBRE}: declaración sin alias, desaconsejada por §2.4 pero legal en PlantUML. */
    private static final Pattern DECLARACION_SIN_ALIAS = Pattern.compile("^state\\s+(\\w+)\\s*$");

    /** {@code A --> B}, admitiendo dirección y color: {@code A -down-> B}, {@code A -[#red]-> B}. */
    private static final Pattern TRANSICION = Pattern.compile(
            "^(\\[\\*\\]|\\w+)\\s*(?:<-|-)-*(?:\\[[^\\]]*\\])?-*(?:up|down|left|right|u|d|l|r)?-*(?:->|-)\\s*"
            + "(\\[\\*\\]|\\w+)\\s*$");

    /** {@code ALIAS : texto}, la anotación que cuelga una línea de un estado (p.ej. {@code : closed}). */
    private static final Pattern ANOTACION = Pattern.compile("^(\\w+)\\s*$");

    /** Directivas de PlantUML que no nombran estados. */
    private static final Set<String> PALABRAS_IGNORADAS = Set.of(
            "note", "end", "skinparam", "title", "header", "footer", "legend", "hide", "show",
            "scale", "caption", "left", "right", "center", "top", "bottom", "together", "package");

    private static final Map<Path, List<Referencia>> cache = new HashMap<>();

    private DiagramaDeEstados() {}

    /** Dónde nombra el diagrama a un estado, para poder señalar la línea en el mensaje de error. */
    public record Referencia(String alias, int linea, boolean declaracion) {

        @Override
        public String toString() {
            return alias + " (línea " + linea + ")";
        }
    }

    /** El {@code estados.puml} del tipo, exista o no. */
    public static Path path(TipoExpedienteInstanceFile tipo) {
        return TiposExpediente.carpeta(tipo).resolve(NOMBRE_FICHERO);
    }

    public static boolean existe(TipoExpedienteInstanceFile tipo) {
        return Files.isRegularFile(path(tipo));
    }

    /** Ruta relativa legible del diagrama, para los mensajes de error. */
    public static String fichero(TipoExpedienteInstanceFile tipo) {
        return TiposExpediente.rel(path(tipo));
    }

    /**
     * Cada vez que el diagrama nombra un estado, en orden de aparición y sin deduplicar: un mismo
     * estado se declara una vez y se usa en tantas transiciones como tenga. Lista vacía si el
     * fichero no existe. Cacheado.
     */
    public static synchronized List<Referencia> referencias(TipoExpedienteInstanceFile tipo) {
        Path path = path(tipo);
        List<Referencia> referencias = cache.get(path);
        if (referencias == null) {
            referencias = Files.isRegularFile(path) ? parse(path) : List.of();
            cache.put(path, referencias);
        }

        return referencias;
    }

    /** Los estados distintos que nombra el diagrama, en orden de aparición. */
    public static Set<String> alias(TipoExpedienteInstanceFile tipo) {
        Set<String> alias = new LinkedHashSet<>();
        for (Referencia referencia : referencias(tipo)) {
            alias.add(referencia.alias());
        }

        return alias;
    }

    /**
     * El alias con el que el diagrama debe nombrar a ese estado: {@code <FASE>_<ESTADO>}. Es la
     * misma composición que exige §2.4, y la que hace comparables el XML y el diagrama.
     */
    public static String alias(State state) {
        return state.getFase().getName() + "_" + state.getName();
    }

    /** Los alias de todos los estados que el XML declara, en orden de fase y estado. */
    public static Set<String> aliasDelXml(TipoExpedienteInstanceFile tipo) {
        Set<String> alias = new LinkedHashSet<>();
        for (Fase fase : tipo.getFases()) {
            for (State state : fase.getStates()) {
                alias.add(alias(state));
            }
        }

        return alias;
    }

    // -----------------------------------------------------------------------------------------
    // Parseo
    // -----------------------------------------------------------------------------------------

    private static List<Referencia> parse(Path path) {
        List<Referencia> referencias = new ArrayList<>();
        boolean enComentarioDeBloque = false;
        int numeroDeLinea = 0;

        for (String original : lineas(path)) {
            numeroDeLinea++;
            String linea = original.trim();

            if (enComentarioDeBloque) {
                enComentarioDeBloque = !linea.contains("'/");
                continue;
            }
            if (linea.startsWith("/'")) {
                enComentarioDeBloque = !linea.contains("'/");
                continue;
            }
            if (esIgnorable(linea)) {
                continue;
            }

            parseLinea(limpia(linea), numeroDeLinea, referencias);
        }

        return referencias;
    }

    private static void parseLinea(String linea, int numeroDeLinea, List<Referencia> referencias) {
        // Un compuesto es la fase, no un estado: 'state RECEPCION {'.
        if (linea.endsWith("{")) {
            return;
        }

        // La etiqueta de una transición o de una anotación va tras el primer ':' y no nombra estados.
        String antesDeDosPuntos = linea.split(":", 2)[0].trim();
        boolean tieneEtiqueta = linea.contains(":");

        Matcher declaracion = DECLARACION_CON_ALIAS.matcher(antesDeDosPuntos);
        if (declaracion.matches()) {
            referencias.add(new Referencia(declaracion.group(1), numeroDeLinea, true));
            return;
        }

        Matcher transicion = TRANSICION.matcher(antesDeDosPuntos);
        if (transicion.matches()) {
            anota(transicion.group(1), numeroDeLinea, referencias);
            anota(transicion.group(2), numeroDeLinea, referencias);
            return;
        }

        Matcher sinAlias = DECLARACION_SIN_ALIAS.matcher(antesDeDosPuntos);
        if (sinAlias.matches()) {
            referencias.add(new Referencia(sinAlias.group(1), numeroDeLinea, true));
            return;
        }

        // 'TRAMITACION_ACEPTADO : closed' — un identificador suelto solo nombra un estado si cuelga
        // de él una etiqueta; sin ella no es nada.
        Matcher anotacion = ANOTACION.matcher(antesDeDosPuntos);
        if (tieneEtiqueta && anotacion.matches()) {
            anota(anotacion.group(1), numeroDeLinea, referencias);
        }
    }

    /** Registra un extremo, salvo que sea el pseudo-estado [*], que no es un estado del XML. */
    private static void anota(String alias, int numeroDeLinea, List<Referencia> referencias) {
        if (!alias.equals("[*]")) {
            referencias.add(new Referencia(alias, numeroDeLinea, false));
        }
    }

    /** Quita lo que decora un estado sin nombrarlo: estereotipos y color. */
    private static String limpia(String linea) {
        return linea.replaceAll("<<[^>]*>>", "").replaceAll("#\\w+", "").trim();
    }

    private static boolean esIgnorable(String linea) {
        if (linea.isEmpty() || linea.startsWith("'") || linea.startsWith("@") || linea.startsWith("!")) {
            return true;
        }
        String primera = linea.split("[\\s:]", 2)[0].toLowerCase();

        return PALABRAS_IGNORADAS.contains(primera) || linea.equals("}");
    }

    private static List<String> lineas(Path path) {
        try {
            return Files.readAllLines(path, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("estados.puml no legible: " + path + " -> " + ex.getMessage(), ex);
        }
    }
}
