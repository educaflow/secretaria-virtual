package com.educaflow.tiposexpedientes.states;

import com.educaflow.common.buildtools.files.tipoexpediente.Fase;
import com.educaflow.common.buildtools.files.tipoexpediente.TipoExpedienteInstanceFile;
import com.educaflow.subsystem.expedientes.services.eventmanager.Phase;
import com.educaflow.subsystem.expedientes.services.eventmanager.State;
import com.educaflow.subsystem.expedientes.services.eventmanager.TipoExpedienteStates;
import com.educaflow.tiposexpedientes.support.TiposExpediente;
import com.educaflow.tiposexpedientes.support.Violacion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * La clase {@code States} que el build genera de cada tipo de expediente concuerda con su
 * {@code TipoExpedienteInstance.xml}: fases, estados y los metadatos de cada estado.
 *
 * <p>A diferencia de {@code PhaseEventManagerTest} y {@code StateEventValidatorTest}, este test <b>no</b>
 * usa el {@code ClassFileImporter} de ArchUnit: ArchUnit expone estructura (tipos, miembros,
 * firmas), y los valores por constante — {@code initial}, {@code closed}, el perfil y los eventos —
 * son argumentos de constructor enterrados en el {@code <clinit>} del bytecode, invisibles para él.
 * Como {@code States} es pura a propósito (sin JPA ni sesión; del dominio solo el enum plano
 * {@code Profile}) y está en el classpath de los tests, aquí se carga con reflexión normal y se
 * comparan los valores que devuelven sus getters.
 *
 * <p><b>Estos tests se escriben A MANO.</b> No son una proyección de ningún catálogo markdown: este
 * fichero es la fuente de verdad y se edita directamente.
 */
class StatesTest {

    private static final String CLASE_STATES = "States";

    // -----------------------------------------------------------------------------------------
    // S1 — fases
    // -----------------------------------------------------------------------------------------

    @Test
    @DisplayName("S1: States tiene las fases del XML, con su código y su nombre, en orden de declaración")
    void s1_lasFasesConcuerdanConElXml() {
        List<Violacion> violaciones = new ArrayList<>();

        for (TipoExpedienteInstanceFile tipo : TiposExpediente.all()) {
            Optional<TipoExpedienteStates> states = states(tipo, violaciones);
            if (states.isEmpty()) {
                continue;
            }

            List<Phase> phases = states.get().getPhases();
            List<Fase> fases = tipo.getFases();

            if (phases.size() != fases.size()) {
                violaciones.add(violacion(tipo, "States declara " + phases.size() + " fase(s) "
                        + codigosDeFase(phases) + " y el XML " + fases.size() + " (" + fases + ")"));
                continue;
            }

            for (int i = 0; i < fases.size(); i++) {
                Fase fase = fases.get(i);
                Phase phase = phases.get(i);

                if (!fase.getName().equals(phase.getCode())) {
                    violaciones.add(violacion(tipo, "la fase " + i + " de States es '" + phase.getCode()
                            + "' y en el XML es '" + fase.getName() + "' (el orden debe ser el de declaración)"));
                    continue;
                }
                if (!fase.getTitleOrHumanizedName().equals(phase.getName())) {
                    violaciones.add(violacion(tipo, "la fase '" + fase.getName() + "' se llama '" + phase.getName()
                            + "' en States y '" + fase.getTitleOrHumanizedName() + "' según el XML"));
                }
            }
        }

        Violacion.assertNone("[S1] Las fases de la clase States generada deben ser las del"
                + " TipoExpedienteInstance.xml, con su código y su nombre, en orden de declaración.", violaciones);
    }

    // -----------------------------------------------------------------------------------------
    // S2 — estados de cada fase
    // -----------------------------------------------------------------------------------------

    @Test
    @DisplayName("S2: cada fase de States tiene los estados de esa fase en el XML, en orden de declaración")
    void s2_losEstadosDeCadaFaseConcuerdanConElXml() {
        List<Violacion> violaciones = new ArrayList<>();

        for (TipoExpedienteInstanceFile tipo : TiposExpediente.all()) {
            Optional<TipoExpedienteStates> states = states(tipo, violaciones);
            if (states.isEmpty()) {
                continue;
            }

            for (Fase fase : tipo.getFases()) {
                Optional<Phase> phase = states.get().getPhase(fase.getName());
                if (phase.isEmpty()) {
                    violaciones.add(violacion(tipo, "States no encuentra la fase '" + fase.getName() + "'"));
                    continue;
                }

                List<String> declarados = new ArrayList<>();
                for (State state : phase.get().getStates()) {
                    declarados.add(state.getCode());
                }

                List<String> esperados = new ArrayList<>();
                for (com.educaflow.common.buildtools.files.tipoexpediente.State state : fase.getStates()) {
                    esperados.add(state.getName());
                }

                if (!declarados.equals(esperados)) {
                    violaciones.add(violacion(tipo, "los estados de la fase '" + fase.getName() + "' son "
                            + declarados + " en States y " + esperados + " en el XML"));
                }
            }

            // Y ninguno de más: getStates() es la unión de todas las fases.
            int totalXml = tipo.getStates().size();
            if (states.get().getStates().size() != totalXml) {
                violaciones.add(violacion(tipo, "States tiene " + states.get().getStates().size()
                        + " estado(s) en total y el XML " + totalXml));
            }
        }

        Violacion.assertNone("[S2] Cada fase de la clase States generada debe tener exactamente los estados que el"
                + " TipoExpedienteInstance.xml declara dentro de esa fase, en orden de declaración.", violaciones);
    }

    // -----------------------------------------------------------------------------------------
    // S3 — metadatos de cada estado
    // -----------------------------------------------------------------------------------------

    @Test
    @DisplayName("S3: cada estado de States lleva el nombre, el perfil, los eventos, initial y closed del XML")
    void s3_losMetadatosDeCadaEstadoConcuerdanConElXml() {
        List<Violacion> violaciones = new ArrayList<>();

        for (TipoExpedienteInstanceFile tipo : TiposExpediente.all()) {
            Optional<TipoExpedienteStates> states = states(tipo, violaciones);
            if (states.isEmpty()) {
                continue;
            }

            for (Fase fase : tipo.getFases()) {
                for (com.educaflow.common.buildtools.files.tipoexpediente.State esperado : fase.getStates()) {
                    Optional<State> declarado = states.get().getState(fase.getName(), esperado.getName());
                    if (declarado.isEmpty()) {
                        violaciones.add(violacion(tipo, "States no encuentra el estado '" + fase.getName()
                                + "/" + esperado.getName() + "'"));
                        continue;
                    }

                    State state = declarado.get();
                    String donde = "el estado '" + fase.getName() + "/" + esperado.getName() + "'";

                    if (!fase.getName().equals(state.getPhase().getCode())) {
                        violaciones.add(violacion(tipo, donde + " dice pertenecer a la fase '"
                                + state.getPhase().getCode() + "'"));
                    }
                    if (!esperado.getTitleOrHumanizedName().equals(state.getName())) {
                        violaciones.add(violacion(tipo, donde + " se llama '" + state.getName()
                                + "' y según el XML '" + esperado.getTitleOrHumanizedName() + "'"));
                    }
                    if (!perfilDelXml(esperado).equals(perfilDeStates(state))) {
                        violaciones.add(violacion(tipo, donde + " tiene el perfil " + perfilDeStates(state)
                                + " y en el XML " + perfilDelXml(esperado)));
                    }
                    if (esperado.isInitial() != state.isInitial()) {
                        violaciones.add(violacion(tipo, donde + " tiene initial=" + state.isInitial()
                                + " y en el XML initial=" + esperado.isInitial()));
                    }
                    if (esperado.isClosed() != state.isFinal()) {
                        violaciones.add(violacion(tipo, donde + " tiene closed=" + state.isFinal()
                                + " y en el XML closed=" + esperado.isClosed()));
                    }

                    List<String> eventosEsperados = (esperado.getEvents() == null) ? List.of() : esperado.getEvents();
                    List<String> eventosDeclarados = new ArrayList<>(state.getEvents());
                    if (!eventosDeclarados.equals(eventosEsperados)) {
                        violaciones.add(violacion(tipo, donde + " tiene los eventos " + eventosDeclarados
                                + " y en el XML " + eventosEsperados));
                    }
                }
            }
        }

        Violacion.assertNone("[S3] Cada estado de la clase States generada debe llevar el nombre, el perfil, los"
                + " eventos y los indicadores initial/closed que dice su estado del TipoExpedienteInstance.xml.",
                violaciones);
    }

    // -----------------------------------------------------------------------------------------
    // S4 — estado inicial
    // -----------------------------------------------------------------------------------------

    @Test
    @DisplayName("S4: el estado inicial de States es el que el XML marca con initial=\"true\"")
    void s4_elEstadoInicialEsElDelXml() {
        List<Violacion> violaciones = new ArrayList<>();

        for (TipoExpedienteInstanceFile tipo : TiposExpediente.all()) {
            Optional<TipoExpedienteStates> states = states(tipo, violaciones);
            if (states.isEmpty()) {
                continue;
            }

            com.educaflow.common.buildtools.files.tipoexpediente.State esperado = tipo.getInitialState();
            State declarado = states.get().getInitialState();

            String esperadoTexto = esperado.getFase().getName() + "/" + esperado.getName();
            String declaradoTexto = declarado.getPhase().getCode() + "/" + declarado.getCode();

            if (!esperadoTexto.equals(declaradoTexto)) {
                violaciones.add(violacion(tipo, "el estado inicial de States es '" + declaradoTexto
                        + "' y el del XML '" + esperadoTexto + "'"));
            }
            if (!declarado.isInitial()) {
                violaciones.add(violacion(tipo, "el estado inicial de States ('" + declaradoTexto
                        + "') no está marcado como initial"));
            }
        }

        Violacion.assertNone("[S4] El getInitialState() de la clase States generada debe ser el único estado que el"
                + " TipoExpedienteInstance.xml marca con initial=\"true\".", violaciones);
    }

    // -----------------------------------------------------------------------------------------
    // S5 — CODE y NAME
    // -----------------------------------------------------------------------------------------

    @Test
    @DisplayName("S5: las constantes CODE y NAME de States son el code y el name del tipo de expediente")
    void s5_codeYNameSonLosDelTipoDeExpediente() {
        List<Violacion> violaciones = new ArrayList<>();

        for (TipoExpedienteInstanceFile tipo : TiposExpediente.all()) {
            Class<?> clase;
            try {
                clase = Class.forName(fqcnStates(tipo));
            } catch (ClassNotFoundException ex) {
                violaciones.add(violacion(tipo, "no existe la clase " + fqcnStates(tipo)));
                continue;
            }

            String code = constante(clase, "CODE", tipo, violaciones);
            String name = constante(clase, "NAME", tipo, violaciones);

            if ((code != null) && (!tipo.getCode().equals(code))) {
                violaciones.add(violacion(tipo, "States.CODE es '" + code + "' y el code del tipo '" + tipo.getCode() + "'"));
            }
            if ((name != null) && (!tipo.getName().equals(name))) {
                violaciones.add(violacion(tipo, "States.NAME es '" + name + "' y el name del tipo '" + tipo.getName() + "'"));
            }
        }

        Violacion.assertNone("[S5] Las constantes CODE y NAME de la clase States generada deben ser el code y el name"
                + " del tipo de expediente.", violaciones);
    }

    // -----------------------------------------------------------------------------------------
    // Ayudas
    // -----------------------------------------------------------------------------------------

    /** El INSTANCE de la clase States del tipo, o vacío anotando la violación. */
    private static Optional<TipoExpedienteStates> states(TipoExpedienteInstanceFile tipo, List<Violacion> violaciones) {
        String fqcn = fqcnStates(tipo);
        try {
            Class<?> clase = Class.forName(fqcn);

            return Optional.of((TipoExpedienteStates) clase.getField("INSTANCE").get(null));
        } catch (Exception ex) {
            violaciones.add(violacion(tipo, "no se ha podido obtener el INSTANCE de " + fqcn
                    + " (¿ha corrido GenerateStatesTask?): " + ex));

            return Optional.empty();
        }
    }

    private static String constante(Class<?> clase, String nombre, TipoExpedienteInstanceFile tipo, List<Violacion> violaciones) {
        try {
            return (String) clase.getField(nombre).get(null);
        } catch (Exception ex) {
            violaciones.add(violacion(tipo, "no existe la constante " + nombre + " en " + clase.getName()));

            return null;
        }
    }

    private static String fqcnStates(TipoExpedienteInstanceFile tipo) {
        return tipo.getBasePackageName() + "." + CLASE_STATES;
    }

    /** El perfil tal cual lo dice el XML, normalizado a "(ninguno)" cuando no declara ninguno. */
    private static String perfilDelXml(com.educaflow.common.buildtools.files.tipoexpediente.State state) {
        String profile = state.getProfile();

        return ((profile == null) || (profile.isBlank())) ? "(ninguno)" : profile;
    }

    private static String perfilDeStates(State state) {
        return (state.getProfile() == null) ? "(ninguno)" : state.getProfile().name();
    }

    private static List<String> codigosDeFase(List<Phase> phases) {
        List<String> codigos = new ArrayList<>();
        for (Phase phase : phases) {
            codigos.add(phase.getCode());
        }

        return codigos;
    }

    private static Violacion violacion(TipoExpedienteInstanceFile tipo, String detalle) {
        return new Violacion(tipo.getCode(), TiposExpediente.rel(tipo.getPath()), detalle);
    }
}
