package com.educaflow.tiposexpedientes.higiene;

import com.educaflow.common.buildtools.files.tipoexpediente.Fase;
import com.educaflow.common.buildtools.files.tipoexpediente.State;
import com.educaflow.common.buildtools.files.tipoexpediente.TipoExpedienteInstanceFile;
import com.educaflow.common.buildtools.files.tipoexpediente.TipoExpedienteInstanceFileFinder;
import com.educaflow.tiposexpedientes.support.DiagramaDeEstados;
import com.educaflow.tiposexpedientes.support.TiposExpediente;
import com.educaflow.tiposexpedientes.support.Violacion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * El diagrama de estados {@code estados.puml} de cada tipo de expediente existe y sus estados son
 * exactamente los del {@code TipoExpedienteInstance.xml} que tiene al lado.
 *
 * <p>El diagrama es la máquina de estados <b>completa</b>: el XML declara las fases, los estados y
 * los eventos de cada estado, pero <b>no a dónde lleva cada evento</b> —eso lo decide el
 * {@code updateState} del {@code PhaseEventManagerImpl} correspondiente—, así que el único sitio
 * donde el recorrido entero se ve de una vez es el {@code .puml}. Por eso es el primer fichero que
 * se escribe al crear un tipo ({@code k-tipo-expediente} §4, paso 2) y el que el build renderiza a
 * {@code estados.png}.
 *
 * <p>Y por eso mismo es un documento peligroso: es lo que se mira para entender el trámite, y nada
 * lo obliga a seguir siendo verdad. Un estado que se añade al XML y no se dibuja, o uno que se
 * quita del XML y se queda en el dibujo, no rompen nada —el diagrama no se compila— y dejan la
 * única vista de conjunto mintiendo. De ahí las tres reglas, en las dos direcciones:
 *
 * <ul>
 *   <li><b>D1</b>: el diagrama existe.</li>
 *   <li><b>D2</b>: todo estado del diagrama está en el XML. Caza el estado borrado del XML que sigue
 *       dibujado y, sobre todo, el <b>typo</b>: en PlantUML un identificador usado sin declarar no
 *       es un error, crea un nodo nuevo en silencio, así que una transición a un destino mal escrito
 *       dibuja un estado que no existe sin quejarse nadie.</li>
 *   <li><b>D3</b>: todo estado del XML está en el diagrama. Caza el estado nuevo que nadie dibujó:
 *       el diagrama sigue siendo correcto en lo que enseña, pero ya no es la máquina entera.</li>
 * </ul>
 *
 * <p>La comparación va por el <b>alias</b> {@code <FASE>_<ESTADO>}, que es como el diagrama nombra a
 * un estado (§2.4): en PlantUML el identificador es global, mientras que un estado del XML se
 * identifica por la pareja (fase, estado) y su nombre solo es único dentro de su fase.
 *
 * <p>Lo que estas reglas <b>no</b> comprueban son las transiciones: su destino no está en el XML, de
 * modo que no hay con qué contrastarlas. Que un evento lleve al estado que dice el dibujo sigue sin
 * verificarlo nadie.
 *
 * <p>Las reglas miran el <b>árbol de fuentes</b>, igual que {@link ClasesDeFaseHuerfanasTest} y por
 * el mismo motivo: lo que hay que denunciar es el fichero que está —o falta— en git.
 *
 * <p><b>Estos tests se escriben A MANO.</b> Este fichero es la fuente de verdad y se edita
 * directamente.
 */
class DiagramaDeEstadosTest {

    // -----------------------------------------------------------------------------------------
    // D1 — el diagrama existe
    // -----------------------------------------------------------------------------------------

    @Test
    @DisplayName("D1: cada tipo de expediente tiene su estados.puml junto al TipoExpedienteInstance.xml")
    void d1_cadaTipoTieneSuDiagramaDeEstados() {
        List<Violacion> violaciones = new ArrayList<>();

        for (TipoExpedienteInstanceFile tipo : TiposExpediente.all()) {
            if (DiagramaDeEstados.existe(tipo)) {
                continue;
            }

            violaciones.add(new Violacion(tipo.getCode(), DiagramaDeEstados.fichero(tipo),
                    "falta el diagrama de estados del tipo, que va junto a su "
                    + TipoExpedienteInstanceFileFinder.TIPO_EXPEDIENTE_XML_NAME + ": es el único sitio donde"
                    + " se ve la máquina entera, porque el destino de cada evento no está en el XML sino en"
                    + " el updateState de su PhaseEventManagerImpl. Este es el esqueleto con las fases y los"
                    + " estados que el XML ya declara; las transiciones van comentadas porque su destino hay"
                    + " que ponerlo a mano:\n" + plantilla(tipo)));
        }

        Violacion.assertNone("[D1] Cada tipo de expediente debe tener un estados.puml en la raíz de su carpeta"
                + " de versión, junto al " + TipoExpedienteInstanceFileFinder.TIPO_EXPEDIENTE_XML_NAME + ":"
                + " es el diagrama de la máquina de estados completa y lo que el build renderiza a"
                + " estados.png.", violaciones);
    }

    // -----------------------------------------------------------------------------------------
    // D2 — ningún estado dibujado que no exista en el XML
    // -----------------------------------------------------------------------------------------

    @Test
    @DisplayName("D2: todo estado que nombra el estados.puml existe en el TipoExpedienteInstance.xml")
    void d2_todoEstadoDelDiagramaExisteEnElXml() {
        List<Violacion> violaciones = new ArrayList<>();

        for (TipoExpedienteInstanceFile tipo : TiposExpediente.all()) {
            if (!DiagramaDeEstados.existe(tipo)) {
                continue; // el tipo no tiene diagrama: ya lo reporta D1
            }
            Set<String> delXml = DiagramaDeEstados.aliasDelXml(tipo);

            for (Map.Entry<String, List<Integer>> sobrante : sobrantes(tipo, delXml).entrySet()) {
                violaciones.add(new Violacion(tipo.getCode(), DiagramaDeEstados.fichero(tipo),
                        "el diagrama nombra el estado '" + sobrante.getKey() + "' (" + lineas(sobrante.getValue())
                        + "), que no existe en el "
                        + TipoExpedienteInstanceFileFinder.TIPO_EXPEDIENTE_XML_NAME + ". O es un estado que se"
                        + " quitó del XML y se quedó dibujado, o está mal escrito: en PlantUML un identificador"
                        + " usado sin declarar no da error, crea un nodo nuevo en silencio. Los estados del XML"
                        + " son " + delXml));
            }
        }

        Violacion.assertNone("[D2] Todo estado que nombre el estados.puml —lo declare (state \"<ESTADO>\" as"
                + " <FASE>_<ESTADO>) o solo lo use en una transición o en una anotación— debe ser un estado"
                + " declarado en el " + TipoExpedienteInstanceFileFinder.TIPO_EXPEDIENTE_XML_NAME + " de su"
                + " tipo: si no, el diagrama enseña un estado por el que ningún expediente pasa.", violaciones);
    }

    // -----------------------------------------------------------------------------------------
    // D3 — ningún estado del XML sin dibujar
    // -----------------------------------------------------------------------------------------

    @Test
    @DisplayName("D3: todo estado del TipoExpedienteInstance.xml aparece en el estados.puml")
    void d3_todoEstadoDelXmlApareceEnElDiagrama() {
        List<Violacion> violaciones = new ArrayList<>();

        for (TipoExpedienteInstanceFile tipo : TiposExpediente.all()) {
            if (!DiagramaDeEstados.existe(tipo)) {
                continue; // el tipo no tiene diagrama: ya lo reporta D1
            }
            Set<String> dibujados = DiagramaDeEstados.alias(tipo);

            for (Fase fase : tipo.getFases()) {
                for (State state : fase.getStates()) {
                    String alias = DiagramaDeEstados.alias(state);
                    if (dibujados.contains(alias)) {
                        continue;
                    }

                    violaciones.add(new Violacion(tipo.getCode(), DiagramaDeEstados.fichero(tipo),
                            "el estado '" + state.getName() + "' de la fase " + fase.getName() + " no aparece"
                            + " en el diagrama, así que la máquina que se enseña no es la que se ejecuta."
                            + " Añádelo dentro de 'state " + fase.getName() + " {' y dibuja sus transiciones"
                            + " (" + (state.getEvents().isEmpty() ? "no declara eventos" : "eventos: "
                            + state.getEvents()) + "):\n"
                            + "\n    " + declaracion(state)
                            + (state.isClosed() ? ("\n    " + alias + " : closed") : "")));
                }
            }
        }

        Violacion.assertNone("[D3] Todo estado declarado en el "
                + TipoExpedienteInstanceFileFinder.TIPO_EXPEDIENTE_XML_NAME + " debe aparecer en el"
                + " estados.puml de su tipo con el alias <FASE>_<ESTADO>: un estado sin dibujar deja la única"
                + " vista de conjunto de la máquina incompleta.", violaciones);
    }

    // -----------------------------------------------------------------------------------------
    // Ayudas
    // -----------------------------------------------------------------------------------------

    /** Los estados que nombra el diagrama y no están en el XML, con las líneas donde aparecen. */
    private static Map<String, List<Integer>> sobrantes(TipoExpedienteInstanceFile tipo, Set<String> delXml) {
        Map<String, List<Integer>> sobrantes = new LinkedHashMap<>();
        for (DiagramaDeEstados.Referencia referencia : DiagramaDeEstados.referencias(tipo)) {
            if (delXml.contains(referencia.alias())) {
                continue;
            }
            sobrantes.computeIfAbsent(referencia.alias(), alias -> new ArrayList<>()).add(referencia.linea());
        }

        return sobrantes;
    }

    private static String lineas(List<Integer> lineas) {
        String numeros = lineas.stream().map(String::valueOf).collect(Collectors.joining(", "));

        return ((lineas.size() == 1) ? "línea " : "líneas ") + numeros;
    }

    /** La línea que declara el estado en el diagrama, con su alias y su nombre corto como etiqueta. */
    private static String declaracion(State state) {
        return "state \"" + state.getName() + "\" as " + DiagramaDeEstados.alias(state);
    }

    /**
     * El esqueleto del diagrama derivado del XML: las fases como estados compuestos, cada estado con
     * su alias, el inicial, los cerrados y una línea comentada por cada transición pendiente de
     * destino.
     */
    private static String plantilla(TipoExpedienteInstanceFile tipo) {
        StringBuilder plantilla = new StringBuilder();
        plantilla.append("\n    @startuml");
        plantilla.append("\n");

        for (Fase fase : tipo.getFases()) {
            plantilla.append("\n    state ").append(fase.getName()).append(" {");
            for (State state : fase.getStates()) {
                plantilla.append("\n        ").append(declaracion(state));
            }
            plantilla.append("\n    }");
            plantilla.append("\n");
        }

        State inicial = tipo.getInitialState();
        if (inicial != null) {
            plantilla.append("\n    [*] --> ").append(DiagramaDeEstados.alias(inicial));
        }
        for (Fase fase : tipo.getFases()) {
            for (State state : fase.getStates()) {
                for (String evento : state.getEvents()) {
                    plantilla.append("\n    ' ").append(DiagramaDeEstados.alias(state))
                             .append(" --> <FASE>_<ESTADO DESTINO> : ").append(evento);
                }
            }
        }

        plantilla.append("\n");
        for (Fase fase : tipo.getFases()) {
            for (State state : fase.getStates()) {
                if (state.isClosed()) {
                    plantilla.append("\n    ").append(DiagramaDeEstados.alias(state)).append(" : closed");
                }
            }
        }

        plantilla.append("\n");
        plantilla.append("\n    @enduml");

        return plantilla.toString();
    }
}
