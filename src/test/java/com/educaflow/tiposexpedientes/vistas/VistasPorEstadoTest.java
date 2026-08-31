package com.educaflow.tiposexpedientes.vistas;

import com.educaflow.common.buildtools.files.tipoexpediente.Fase;
import com.educaflow.common.buildtools.files.tipoexpediente.State;
import com.educaflow.tiposexpedientes.support.FormDeEstado;
import com.educaflow.tiposexpedientes.support.TiposExpediente;
import com.educaflow.tiposexpedientes.support.Violacion;
import com.educaflow.tiposexpedientes.support.ViewsDeFase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Cada estado de cada fase tiene en el {@code views.xml} de su fase los {@code <form state="…">}
 * que el runtime va a buscar.
 *
 * <p>Es el hueco que el propio skill `k-tipo-expediente` marca como "lo que no comprueba nada y solo
 * falla en runtime" (§3.4): el build valida que un form de estado esté en la carpeta de su fase y
 * que sus paneles existan —o sea, lo que <b>sobra</b>—, pero nada mira lo que <b>falta</b>. Y lo que
 * falta no se nota hasta que alguien navega a ese estado: {@code PhaseEventManager.getViewName}
 * compone el nombre, no encuentra la vista y lanza "No existe la vista en el expediente".
 *
 * <p>Cómo elige el runtime, que es de donde salen las dos reglas:
 *
 * <ol>
 *   <li>busca {@code exp-<Code>-<FASE>-<ESTADO>-<PROFILE>-form}, con el perfil <b>actuante</b>;</li>
 *   <li>si no existe, cae en la genérica {@code exp-<Code>-<FASE>-<ESTADO>-form};</li>
 *   <li>si tampoco, excepción.</li>
 * </ol>
 *
 * <p>De ahí X1: el perfil actuante puede ser <b>cualquiera</b> de los del tipo (el controlador solo
 * comprueba que el perfil de la petición lo use algún estado del tipo, no que sea el del estado
 * actual: hay listados que abren con perfil {@code RESPONSABLE} expedientes en estados de perfil
 * {@code CREADOR}), así que la genérica es la red de seguridad y no puede faltar en ningún estado.
 *
 * <p>Y de ahí X2: si el estado tiene dueño y eventos pero no tiene la vista de su dueño, el dueño
 * cae en la genérica —que es la de solo lectura— y el expediente se queda atascado <b>sin ningún
 * error</b>: nadie puede dispararle sus eventos. Es el peor de los dos fallos, porque no se ve.
 *
 * <p><b>Estos tests se escriben A MANO.</b> No son una proyección de ningún catálogo markdown, al
 * contrario que {@code com.educaflow.architecture} y {@code com.educaflow.views}: este fichero es la
 * fuente de verdad y se edita directamente.
 */
class VistasPorEstadoTest {

    // -----------------------------------------------------------------------------------------
    // X1 — la vista genérica de cada estado
    // -----------------------------------------------------------------------------------------

    @Test
    @DisplayName("X1: cada estado tiene su <form state=\"…\"> genérico, que es al que cae el runtime")
    void x1_cadaEstadoTieneSuFormGenerico() {
        List<Violacion> violaciones = new ArrayList<>();

        for (Fase fase : TiposExpediente.todasLasFases()) {
            for (State state : fase.getStates()) {
                if (tieneForm(fase, state.getName(), "")) {
                    continue;
                }

                violaciones.add(new Violacion(TiposExpediente.nombre(fase), ViewsDeFase.fichero(fase),
                        (ViewsDeFase.existe(fase) ? "" : "no existe el fichero; ")
                        + "falta el form genérico del estado '" + state.getName() + "'"
                        + " (la vista " + nombreVista(fase, state, "") + "), que es la que usa el runtime"
                        + " cuando el perfil actuante no tiene la suya; sin ella, navegar al estado lanza"
                        + " \"No existe la vista en el expediente\":\n" + plantilla(state, "")));
            }
        }

        Violacion.assertNone("[X1] Cada estado de cada fase debe tener en el views.xml de su fase un"
                + " <form state=\"<ESTADO>\"> sin perfil: es la vista de reserva a la que cae"
                + " PhaseEventManager.getViewName cuando no existe la del perfil actuante.", violaciones);
    }

    // -----------------------------------------------------------------------------------------
    // X2 — la vista del perfil dueño de los estados que tienen eventos
    // -----------------------------------------------------------------------------------------

    @Test
    @DisplayName("X2: cada estado con perfil y con eventos tiene el <form state=\"…\" profile=\"…\"> de su perfil")
    void x2_cadaEstadoConEventosTieneElFormDeSuPerfil() {
        List<Violacion> violaciones = new ArrayList<>();

        for (Fase fase : TiposExpediente.todasLasFases()) {
            for (State state : fase.getStates()) {
                String profile = state.getProfile();

                // Sin perfil no hay dueño del turno: la vista de todos es la genérica (X1).
                // Sin eventos no hay nada que disparar: el estado solo se mira.
                if ((profile == null) || (profile.isBlank()) || (state.getEvents().isEmpty())) {
                    continue;
                }
                if (tieneForm(fase, state.getName(), profile)) {
                    continue;
                }

                violaciones.add(new Violacion(TiposExpediente.nombre(fase), ViewsDeFase.fichero(fase),
                        "el estado '" + state.getName() + "' es de " + profile + " y dispara "
                        + state.getEvents() + ", pero no tiene el form de ese perfil"
                        + " (la vista " + nombreVista(fase, state, profile) + "): un usuario "
                        + profile + " caería en la vista genérica de solo lectura y el expediente se"
                        + " quedaría atascado sin ningún error:\n" + plantilla(state, profile)));
            }
        }

        Violacion.assertNone("[X2] Todo estado con profile y con al menos un evento debe tener en el views.xml"
                + " de su fase el <form state=\"<ESTADO>\" profile=\"<PROFILE>\"> de su perfil: es la única vista"
                + " desde la que su dueño puede disparar esos eventos.", violaciones);
    }

    // -----------------------------------------------------------------------------------------
    // X3 — sin forms duplicados
    // -----------------------------------------------------------------------------------------

    @Test
    @DisplayName("X3: no hay dos forms con el mismo (estado, perfil) en la misma fase")
    void x3_noHayDosFormsConElMismoEstadoYPerfil() {
        List<Violacion> violaciones = new ArrayList<>();

        for (Fase fase : TiposExpediente.todasLasFases()) {
            Map<String, List<FormDeEstado>> porVista = new LinkedHashMap<>();
            for (FormDeEstado form : ViewsDeFase.forms(fase)) {
                porVista.computeIfAbsent(form.nombreVista(), clave -> new ArrayList<>()).add(form);
            }

            for (Map.Entry<String, List<FormDeEstado>> entrada : porVista.entrySet()) {
                if (entrada.getValue().size() == 1) {
                    continue;
                }

                violaciones.add(new Violacion(TiposExpediente.nombre(fase), ViewsDeFase.fichero(fase),
                        "hay " + entrada.getValue().size() + " forms " + entrada.getValue().get(0)
                        + ", que producen la misma vista " + entrada.getKey() + ": Axelor se queda con"
                        + " la última y las demás no se pintan nunca"));
            }
        }

        Violacion.assertNone("[X3] Dos <form> de la misma fase no pueden tener el mismo (state, profile):"
                + " el viewprocessor les compone el mismo name de vista y una tapa a la otra en silencio.",
                violaciones);
    }

    // -----------------------------------------------------------------------------------------
    // Ayudas
    // -----------------------------------------------------------------------------------------

    private static boolean tieneForm(Fase fase, String state, String profile) {
        for (FormDeEstado form : ViewsDeFase.formsDelEstado(fase, state)) {
            if (form.profile().equals(profile)) {
                return true;
            }
        }

        return false;
    }

    private static String nombreVista(Fase fase, State state, String profile) {
        return new FormDeEstado(fase, ViewsDeFase.path(fase), state.getName(), profile, List.of()).nombreVista();
    }

    /**
     * El form que falta, listo para pegar en el {@code views.xml} de la fase. Es la misma calidad de
     * diagnóstico que dan las reglas del PhaseEventManager y del validator, que traen el código del
     * método que falta.
     */
    private static String plantilla(State state, String profile) {
        String atributoPerfil = profile.isBlank() ? "" : (" profile=\"" + profile + "\"");

        StringBuilder plantilla = new StringBuilder();
        plantilla.append("\n    <form state=\"").append(state.getName()).append("\"").append(atributoPerfil).append(">");
        plantilla.append("\n        <include-panels>");
        plantilla.append("\n            <!-- los paneles del form plantilla exp-<Code>-Templates de la raíz de la versión -->");
        plantilla.append("\n        </include-panels>");
        plantilla.append("\n        <footer>");
        plantilla.append("\n            <buttons-left/>");
        plantilla.append("\n            <buttons-right>");
        for (String evento : botonesSugeridos(state, profile)) {
            plantilla.append("\n                <button name=\"").append(evento).append("\" colSpan=\"2\" title=\"")
                     .append(evento).append("\" onClick=\"subsysExpedientes-event-action\"/>");
        }
        plantilla.append("\n            </buttons-right>");
        plantilla.append("\n        </footer>");
        plantilla.append("\n    </form>");

        return plantilla.toString();
    }

    /**
     * Los botones del form sugerido: en el del perfil dueño, los eventos del estado; en el genérico,
     * solo {@code EXIT}, porque es la vista de solo lectura de los demás perfiles.
     */
    private static Set<String> botonesSugeridos(State state, String profile) {
        if (profile.isBlank()) {
            return Set.of("EXIT");
        }

        return new LinkedHashSet<>(state.getEvents());
    }
}
