package com.educaflow.tiposexpedientes.vistas;

import com.educaflow.common.buildtools.files.tipoexpediente.Fase;
import com.educaflow.common.buildtools.files.tipoexpediente.State;
import com.educaflow.subsystem.expedientes.services.tramitacion.CommonEvent;
import com.educaflow.tiposexpedientes.support.FormDeEstado;
import com.educaflow.tiposexpedientes.support.TiposExpediente;
import com.educaflow.tiposexpedientes.support.Violacion;
import com.educaflow.tiposexpedientes.support.ViewsDeFase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Los botones del {@code <footer>} de los forms de una fase concuerdan con los eventos que el
 * {@code TipoExpedienteInstance.xml} declara en cada estado.
 *
 * <p>El {@code name} de un botón del footer <b>es</b> el evento que dispara: el cliente lo manda tal
 * cual y {@code Tramitador} lo busca en los eventos del estado actual. Así que las dos direcciones
 * fallan, y ninguna la ve nadie hoy:
 *
 * <ul>
 *   <li><b>Un botón que no es evento del estado</b> (Y1) revienta al pulsarlo, en la cara del
 *       usuario. Incluye el caso del esqueleto sin rellenar, {@code <button name="">}.</li>
 *   <li><b>Un evento sin botón</b> (Y2) es un evento muerto: la regla E1 obliga a escribir su
 *       {@code trigger<Evento>} y la V1 sus reglas de validación, así que el tipo de expediente
 *       parece completo y pasa todos los demás tests, pero no hay forma de dispararlo desde la
 *       aplicación. Es una transición que existe en el diagrama y no existe para el usuario.</li>
 * </ul>
 *
 * <p>Y2 mira la <b>unión</b> de los botones de todos los forms del estado, no form a form: el
 * reparto normal es que los eventos vayan en la vista del perfil dueño y la genérica lleve solo
 * {@code EXIT}, pero un estado sin perfil (que lo hay) tiene que poder llevarlos en la genérica.
 *
 * <p>Los eventos comunes de {@link CommonEvent} son legales en cualquier estado sin declararlos —el
 * runtime los atiende gratis—, así que Y1 los admite siempre. La lista se deriva del enum, no se
 * escribe a mano, para que no pueda separarse de lo que hace el runtime.
 *
 * <p><b>Estos tests se escriben A MANO.</b> No son una proyección de ningún catálogo markdown, al
 * contrario que {@code com.educaflow.architecture} y {@code com.educaflow.views}: este fichero es la
 * fuente de verdad y se edita directamente.
 */
class BotonesDelFooterTest {

    /**
     * La acción que lleva el evento al servidor ({@code ExpedienteController.triggerEvent}), en
     * {@code subsystem/expedientes/controllers/actions-expedientes.xml}. Es la de <b>todos</b> los
     * botones del footer.
     */
    private static final String ACCION_EVENTO = "subsysExpedientes-event-action";

    /** Prefijo de Axelor para encadenar acciones en un mismo onClick. */
    private static final String PREFIJO_SERIAL = "serial:";

    // -----------------------------------------------------------------------------------------
    // Y1 — ningún botón que no sea un evento del estado
    // -----------------------------------------------------------------------------------------

    @Test
    @DisplayName("Y1: el name de cada botón del footer es un evento del estado o un evento común")
    void y1_cadaBotonEsUnEventoDelEstado() {
        List<Violacion> violaciones = new ArrayList<>();

        for (Fase fase : TiposExpediente.todasLasFases()) {
            for (FormDeEstado form : ViewsDeFase.forms(fase)) {
                State state = estado(fase, form.state());
                if (state == null) {
                    continue; // el form no es de ningún estado de la fase: ya lo rechaza el build
                }
                Set<String> admitidos = eventosAdmitidos(state);

                for (FormDeEstado.Boton boton : form.botones()) {
                    if (admitidos.contains(boton.name())) {
                        continue;
                    }

                    String queEs = boton.name().isBlank()
                            ? "un botón sin name (el esqueleto sin rellenar)"
                            : "el botón '" + boton.name() + "', que no es evento del estado";

                    violaciones.add(new Violacion(TiposExpediente.nombre(fase), ViewsDeFase.fichero(fase),
                            form + " tiene " + queEs + ": el name de un botón del footer es el evento que"
                            + " dispara, y al pulsarlo el servidor no lo encuentra. Los eventos del estado '"
                            + state.getName() + "' son " + state.getEvents() + " y los comunes " + comunes()));
                }
            }
        }

        Violacion.assertNone("[Y1] El name de todo botón del <footer> de un form de estado debe ser un evento"
                + " declarado en ese estado o uno de los eventos comunes " + comunes() + ".", violaciones);
    }

    // -----------------------------------------------------------------------------------------
    // Y2 — ningún evento sin botón
    // -----------------------------------------------------------------------------------------

    @Test
    @DisplayName("Y2: cada evento declarado en un estado tiene botón en alguno de los forms de ese estado")
    void y2_cadaEventoTieneSuBoton() {
        List<Violacion> violaciones = new ArrayList<>();

        for (Fase fase : TiposExpediente.todasLasFases()) {
            for (State state : fase.getStates()) {
                List<FormDeEstado> forms = ViewsDeFase.formsDelEstado(fase, state.getName());
                if (forms.isEmpty()) {
                    continue; // el estado no tiene ninguna vista: ya lo reporta X1
                }

                Set<String> conBoton = new LinkedHashSet<>();
                for (FormDeEstado form : forms) {
                    for (FormDeEstado.Boton boton : form.botones()) {
                        conBoton.add(boton.name());
                    }
                }

                for (String evento : state.getEvents()) {
                    if (conBoton.contains(evento)) {
                        continue;
                    }

                    violaciones.add(new Violacion(TiposExpediente.nombre(fase), ViewsDeFase.fichero(fase),
                            "el estado '" + state.getName() + "' declara el evento '" + evento + "' y ningún"
                            + " form suyo tiene un botón que lo dispare, así que el usuario no puede llegar a"
                            + " él (aunque su trigger" + upperCamel(evento) + " exista, por la regla E1)."
                            + " Añade al form " + dondeVaElBoton(state) + ":\n"
                            + "                <button name=\"" + evento + "\" colSpan=\"2\" title=\"" + evento
                            + "\" onClick=\"" + ACCION_EVENTO + "\"/>"));
                }
            }
        }

        Violacion.assertNone("[Y2] Todo evento declarado en un <state> debe tener un botón con ese name en el"
                + " <footer> de alguno de los forms de ese estado: si no, no hay forma de dispararlo desde la"
                + " aplicación.", violaciones);
    }

    // -----------------------------------------------------------------------------------------
    // Y3 — todos los botones disparan la acción de eventos
    // -----------------------------------------------------------------------------------------

    @Test
    @DisplayName("Y3: el onClick de cada botón del footer llama a la acción de eventos del subsistema")
    void y3_cadaBotonLlamaALaAccionDeEventos() {
        List<Violacion> violaciones = new ArrayList<>();

        for (Fase fase : TiposExpediente.todasLasFases()) {
            for (FormDeEstado form : ViewsDeFase.forms(fase)) {
                for (FormDeEstado.Boton boton : form.botones()) {
                    if (acciones(boton.onClick()).contains(ACCION_EVENTO)) {
                        continue;
                    }

                    violaciones.add(new Violacion(TiposExpediente.nombre(fase), ViewsDeFase.fichero(fase),
                            form + ": el botón '" + boton.name() + "' tiene onClick=\"" + boton.onClick()
                            + "\", que no incluye " + ACCION_EVENTO + ", así que no dispara el evento."
                            + " Para encadenar acciones propias antes, ponlas delante:"
                            + " onClick=\"serial:<accion propia>," + ACCION_EVENTO + "\""));
                }
            }
        }

        Violacion.assertNone("[Y3] El onClick de todo botón del <footer> debe incluir " + ACCION_EVENTO
                + ", que es lo que lleva el evento a ExpedienteController.triggerEvent.", violaciones);
    }

    // -----------------------------------------------------------------------------------------
    // Ayudas
    // -----------------------------------------------------------------------------------------

    /** El estado de la fase con ese nombre, o null si el form no corresponde a ninguno. */
    private static State estado(Fase fase, String name) {
        for (State state : fase.getStates()) {
            if (state.getName().equals(name)) {
                return state;
            }
        }

        return null;
    }

    /** Los eventos que puede llevar un botón de ese estado: los suyos más los comunes. */
    private static Set<String> eventosAdmitidos(State state) {
        Set<String> eventos = new LinkedHashSet<>(state.getEvents());
        eventos.addAll(comunes());

        return eventos;
    }

    /** Los eventos que el runtime atiende sin declararlos, derivados del enum para que no diverjan. */
    private static Set<String> comunes() {
        Set<String> comunes = new LinkedHashSet<>();
        for (CommonEvent commonEvent : CommonEvent.values()) {
            comunes.add(commonEvent.name());
        }

        return comunes;
    }

    /** Las acciones de un onClick, que puede ser una sola, una lista separada por comas o un serial:. */
    private static List<String> acciones(String onClick) {
        String valor = onClick.startsWith(PREFIJO_SERIAL) ? onClick.substring(PREFIJO_SERIAL.length()) : onClick;

        List<String> acciones = new ArrayList<>();
        for (String accion : valor.split(",")) {
            acciones.add(accion.trim());
        }

        return acciones;
    }

    /** En qué form se espera el botón, para que el mensaje diga dónde pegarlo. */
    private static String dondeVaElBoton(State state) {
        String profile = state.getProfile();

        return ((profile == null) || (profile.isBlank()))
                ? "genérico del estado (no tiene perfil dueño)"
                : ("<form state=\"" + state.getName() + "\" profile=\"" + profile + "\">");
    }

    private static String upperCamel(String evento) {
        return com.educaflow.common.buildtools.common.TextUtil.getUpperCamelCase(List.of(evento)).get(0);
    }
}
