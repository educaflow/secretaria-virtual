package com.educaflow.tiposexpedientes.phaseeventmanager;

import com.educaflow.common.buildtools.common.TextUtil;
import com.educaflow.common.buildtools.files.phaseeventmanagerfile.PhaseEventManagerFile;
import com.educaflow.common.buildtools.files.tipoexpediente.Fase;
import com.educaflow.subsystem.expedientes.services.eventmanager.OnEnterState;
import com.educaflow.subsystem.expedientes.services.eventmanager.WhenEvent;
import com.educaflow.tiposexpedientes.support.Bytecode;
import com.educaflow.tiposexpedientes.support.TiposExpediente;
import com.educaflow.tiposexpedientes.support.Violacion;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * El {@code PhaseEventManager} de cada <b>fase</b> concuerda con el trozo de máquina de estados que le
 * toca según el {@code TipoExpedienteInstance.xml}: un método por cada evento de la fase y otro por
 * cada estado de la fase, ni más ni menos.
 *
 * <p>Desde que los tipos de expediente están divididos en fases hay un {@code PhaseEventManagerImpl} por
 * fase, en el paquete de la fase, y cada uno atiende <b>solo los estados de su propia fase</b>. Los
 * eventos de una fase son la unión de los eventos de sus estados, porque un evento siempre se
 * dispara desde un estado; un mismo evento presente en dos fases lleva su propio {@code trigger} en
 * cada una.
 *
 * <p>El <b>evento inicial</b> no entra en este reparto: es del tipo de expediente entero y lo
 * atiende un {@code InitialEventManagerImpl} en la raíz de la versión ({@code InitialEventManagerTest}).
 *
 * <p>Los nombres de los métodos usan el código del estado dentro de su fase: la clase ya está en el
 * paquete de su fase y no hay ambigüedad. Que la máquina de estados completa del tipo concuerde con
 * el XML lo comprueba {@code StatesTest}, sobre la clase {@code States} que genera el build.
 *
 * <p>Estas reglas eran hasta ahora una barrera del build (el {@code check()} con Spoon de
 * {@code createfiles}); viven aquí, sobre bytecode, para que la generación de esqueletos y su
 * validación estén separadas.
 *
 * <p><b>Estos tests se escriben A MANO.</b> No son una proyección de ningún catálogo markdown, al
 * contrario que {@code com.educaflow.architecture} y {@code com.educaflow.views}: este fichero es la
 * fuente de verdad y se edita directamente.
 */
class PhaseEventManagerTest {

    private static final String FQCN_EVENT_CONTEXT =
            "com.educaflow.subsystem.expedientes.services.eventmanager.EventContext";
    private static final String FQCN_PHASE_EVENT_MANAGER =
            "com.educaflow.subsystem.expedientes.services.eventmanager.PhaseEventManager";
    private static final String VOID = "void";
    private static final String METODO_TRIGGER_INITIAL_EVENT = "triggerInitialEvent";

    // -----------------------------------------------------------------------------------------
    // E0
    // -----------------------------------------------------------------------------------------

    @Test
    @DisplayName("E0: cada fase tiene compilada la clase de su PhaseEventManager, que extiende PhaseEventManager")
    void e0_existeLaClaseDelPhaseEventManager() {
        List<Violacion> violaciones = new ArrayList<>();

        for (Fase fase : TiposExpediente.todasLasFases()) {
            String fqcn = fase.getFqcnPhaseEventManager();
            Optional<JavaClass> clase = Bytecode.clase(fqcn);

            if (clase.isEmpty()) {
                violaciones.add(new Violacion(TiposExpediente.nombre(fase), ficheroPhaseEventManager(fase),
                        "no existe la clase " + fqcn + " (¿falta compilar, o la carpeta de la fase no se llama"
                        + " como la fase en minúsculas?)"));
            } else if (!clase.get().isAssignableTo(FQCN_PHASE_EVENT_MANAGER)) {
                violaciones.add(new Violacion(TiposExpediente.nombre(fase), ficheroPhaseEventManager(fase),
                        "la clase " + fqcn + " no extiende " + FQCN_PHASE_EVENT_MANAGER));
            }
        }

        Violacion.assertNone("[E0] La clase del PhaseEventManager de cada fase de cada tipo de expediente debe existir y"
                + " extender " + FQCN_PHASE_EVENT_MANAGER + ".", violaciones);
    }

    // -----------------------------------------------------------------------------------------
    // E1 / E2 — eventos
    // -----------------------------------------------------------------------------------------

    @Test
    @DisplayName("E1: por cada evento de la fase hay un método trigger<Evento> con @WhenEvent y la firma correcta")
    void e1_existeUnTriggerPorEvento() {
        List<Violacion> violaciones = new ArrayList<>();

        for (Fase fase : TiposExpediente.todasLasFases()) {
            Optional<JavaClass> clase = Bytecode.clase(fase.getFqcnPhaseEventManager());
            if (clase.isEmpty()) {
                continue; // ya lo reporta E0; no repetimos el mismo fallo en cada regla
            }
            PhaseEventManagerFile phaseEventManagerFile = phaseEventManagerFile(fase);
            String modelo = phaseEventManagerFile.getModelFQCN();

            for (String evento : TextUtil.getUpperCamelCase(fase.getEvents())) {
                String nombreMetodo = PhaseEventManagerFile.getMethodNameTriggerEvent(evento);
                String esperada = Bytecode.firmaEsperada(nombreMetodo, VOID, modelo, modelo, FQCN_EVENT_CONTEXT);

                List<JavaMethod> homonimos = Bytecode.metodosLlamados(clase.get(), nombreMetodo);
                List<JavaMethod> correctos = homonimos.stream()
                        .filter(m -> m.isAnnotatedWith(WhenEvent.class))
                        .filter(m -> Bytecode.tieneFirma(m, VOID, modelo, modelo, FQCN_EVENT_CONTEXT))
                        .toList();

                if (correctos.size() == 1) {
                    continue;
                }

                violaciones.add(new Violacion(TiposExpediente.nombre(fase), ficheroPhaseEventManager(fase),
                        descripcionMetodoIncorrecto(nombreMetodo, "el evento " + evento, esperada,
                                homonimos, correctos.size(), WhenEvent.class)
                        + phaseEventManagerFile.getSourceCodeTriggerMethod(evento)));
            }
        }

        Violacion.assertNone("[E1] Por cada evento de la fase (la unión de los eventos de sus estados) debe haber"
                + " exactamente un método trigger<Evento> anotado @WhenEvent, void y con parámetros"
                + " (<Entidad>, <Entidad>, EventContext) en el PhaseEventManager de esa fase.",
                violaciones);
    }

    @Test
    @DisplayName("E2: no sobra ningún método @WhenEvent que no corresponda a un evento de la fase")
    void e2_noSobraNingunTrigger() {
        List<Violacion> violaciones = new ArrayList<>();

        for (Fase fase : TiposExpediente.todasLasFases()) {
            Optional<JavaClass> clase = Bytecode.clase(fase.getFqcnPhaseEventManager());
            if (clase.isEmpty()) {
                continue;
            }

            Set<String> esperados = new LinkedHashSet<>();
            for (String evento : TextUtil.getUpperCamelCase(fase.getEvents())) {
                esperados.add(PhaseEventManagerFile.getMethodNameTriggerEvent(evento));
            }

            for (JavaMethod metodo : Bytecode.metodosAnotados(clase.get(), WhenEvent.class)) {
                if (!esperados.contains(metodo.getName())) {
                    violaciones.add(new Violacion(TiposExpediente.nombre(fase), ficheroPhaseEventManager(fase),
                            "sobra el método @WhenEvent " + metodo.getName() + "(...): la fase " + fase.getName()
                            + " no tiene ningún evento " + eventoDe(metodo.getName(), "trigger")
                            + ". Sus eventos son: " + fase.getEvents()
                            + " (si ese evento es de otra fase, su trigger va en el PhaseEventManager de esa otra fase)"));
                }
            }
        }

        Violacion.assertNone("[E2] Todo método anotado @WhenEvent debe corresponder a un evento de la propia fase.",
                violaciones);
    }

    // -----------------------------------------------------------------------------------------
    // E3 / E4 — estados
    // -----------------------------------------------------------------------------------------

    @Test
    @DisplayName("E3: por cada estado de la fase hay un método onEnter<Estado> con @OnEnterState y la firma correcta")
    void e3_existeUnOnEnterPorEstado() {
        List<Violacion> violaciones = new ArrayList<>();

        for (Fase fase : TiposExpediente.todasLasFases()) {
            Optional<JavaClass> clase = Bytecode.clase(fase.getFqcnPhaseEventManager());
            if (clase.isEmpty()) {
                continue;
            }
            PhaseEventManagerFile phaseEventManagerFile = phaseEventManagerFile(fase);
            String modelo = phaseEventManagerFile.getModelFQCN();

            for (String estado : TextUtil.getUpperCamelCase(fase.getStates())) {
                String nombreMetodo = PhaseEventManagerFile.getMethodNameOnEnterEvent(estado);
                String esperada = Bytecode.firmaEsperada(nombreMetodo, VOID, modelo, FQCN_EVENT_CONTEXT);

                List<JavaMethod> homonimos = Bytecode.metodosLlamados(clase.get(), nombreMetodo);
                List<JavaMethod> correctos = homonimos.stream()
                        .filter(m -> m.isAnnotatedWith(OnEnterState.class))
                        .filter(m -> Bytecode.tieneFirma(m, VOID, modelo, FQCN_EVENT_CONTEXT))
                        .toList();

                if (correctos.size() == 1) {
                    continue;
                }

                violaciones.add(new Violacion(TiposExpediente.nombre(fase), ficheroPhaseEventManager(fase),
                        descripcionMetodoIncorrecto(nombreMetodo, "el estado " + estado, esperada,
                                homonimos, correctos.size(), OnEnterState.class)
                        + phaseEventManagerFile.getSourceCodeOnEnterMethod(estado)));
            }
        }

        Violacion.assertNone("[E3] Por cada estado de la fase debe haber exactamente un método onEnter<Estado>"
                + " anotado @OnEnterState, void y con parámetros (<Entidad>, EventContext) en el PhaseEventManager de"
                + " esa fase. El nombre del método lleva el nombre corto del estado, sin la fase.",
                violaciones);
    }

    @Test
    @DisplayName("E4: no sobra ningún método @OnEnterState que no corresponda a un estado de la fase")
    void e4_noSobraNingunOnEnter() {
        List<Violacion> violaciones = new ArrayList<>();

        for (Fase fase : TiposExpediente.todasLasFases()) {
            Optional<JavaClass> clase = Bytecode.clase(fase.getFqcnPhaseEventManager());
            if (clase.isEmpty()) {
                continue;
            }

            Set<String> esperados = new LinkedHashSet<>();
            for (String estado : TextUtil.getUpperCamelCase(fase.getStates())) {
                esperados.add(PhaseEventManagerFile.getMethodNameOnEnterEvent(estado));
            }

            for (JavaMethod metodo : Bytecode.metodosAnotados(clase.get(), OnEnterState.class)) {
                if (!esperados.contains(metodo.getName())) {
                    violaciones.add(new Violacion(TiposExpediente.nombre(fase), ficheroPhaseEventManager(fase),
                            "sobra el método @OnEnterState " + metodo.getName() + "(...): la fase " + fase.getName()
                            + " no tiene ningún estado " + eventoDe(metodo.getName(), "onEnter")
                            + ". Sus estados son: " + fase.getStates()
                            + " (el onEnter de un estado de otra fase va en el PhaseEventManager de esa otra fase)"));
                }
            }
        }

        Violacion.assertNone("[E4] Todo método anotado @OnEnterState debe corresponder a un estado de la propia fase.",
                violaciones);
    }

    // -----------------------------------------------------------------------------------------
    // E5 — triggerInitialEvent
    // -----------------------------------------------------------------------------------------

    /**
     * El evento inicial ya no es de ninguna fase: es del <b>tipo de expediente</b>, porque se
     * dispara cuando todavía no hay estado del que partir. Vive en un {@code InitialEventManagerImpl}
     * en la raíz de la versión, y que exista y tenga la firma correcta lo comprueba
     * {@code InitialEventManagerTest}.
     *
     * <p>Lo que se comprueba aquí es la otra mitad: que no quede un {@code triggerInitialEvent} en
     * ningún {@code PhaseEventManagerImpl}. Un método así ya <b>no lo llama nadie</b> —{@code Tramitador}
     * resuelve el {@code InitialEventManager} del tipo, no el {@code PhaseEventManager} de la fase—, así
     * que se quedaría ahí sin ejecutarse nunca; y como la clase base ya no declara el método, no
     * habría ni siquiera un {@code @Override} que fallase al compilar. Es exactamente el fallo que
     * deja un tipo de expediente a medio migrar: la inicialización escrita donde ya no se lee.
     *
     * <p>Que un tipo de expediente <b>no</b> pueda declarar un evento llamado {@code INITIAL_EVENT}
     * —que produciría un {@code triggerInitialEvent} legítimo y esta regla lo daría por sobrante—
     * lo garantiza {@code ApiBaseReservadaTest}, que reserva ese nombre.
     */
    @Test
    @DisplayName("E5: ningún PhaseEventManager de fase declara triggerInitialEvent: el evento inicial es del tipo")
    void e5_ningunPhaseEventManagerDeFaseDeclaraTriggerInitialEvent() {
        List<Violacion> violaciones = new ArrayList<>();

        for (Fase fase : TiposExpediente.todasLasFases()) {
            Optional<JavaClass> clase = Bytecode.clase(fase.getFqcnPhaseEventManager());
            if (clase.isEmpty()) {
                continue;
            }

            for (JavaMethod metodo : Bytecode.metodosLlamados(clase.get(), METODO_TRIGGER_INITIAL_EVENT)) {
                violaciones.add(new Violacion(TiposExpediente.nombre(fase), ficheroPhaseEventManager(fase),
                        "sobra el método " + Bytecode.firma(metodo) + ": el evento inicial es del tipo de"
                        + " expediente, no de una fase, así que este método NO se llama nunca. Muévelo al"
                        + " " + fase.getTipoExpediente().getInitialEventManagerClassName() + " del tipo, que"
                        + " está en la raíz de la carpeta de versión."));
            }
        }

        Violacion.assertNone("[E5] Ningún " + FQCN_PHASE_EVENT_MANAGER + " de fase debe declarar un método"
                + " " + METODO_TRIGGER_INITIAL_EVENT + ": el evento inicial lo atiende el"
                + " InitialEventManager del tipo de expediente.",
                violaciones);
    }

    // -----------------------------------------------------------------------------------------
    // Ayudas
    // -----------------------------------------------------------------------------------------

    /**
     * Explica por qué no vale lo que hay hoy. Distingue los tres casos que el antiguo check() del
     * build confundía en uno solo (y que además reportaba por duplicado, como método que falta y
     * como método que sobra): que no exista nada con ese nombre, que exista sin la anotación, o que
     * exista anotado pero con otra firma.
     */
    private static String descripcionMetodoIncorrecto(String nombreMetodo, String para, String firmaEsperada,
            List<JavaMethod> homonimos, int correctos, Class<? extends Annotation> anotacion) {

        StringBuilder mensaje = new StringBuilder();
        if (homonimos.isEmpty()) {
            mensaje.append("falta el método ").append(nombreMetodo).append(" para ").append(para);
        } else if (correctos == 0) {
            mensaje.append("el método ").append(nombreMetodo).append(" (").append(para)
                   .append(") existe pero no vale:");
            for (JavaMethod metodo : homonimos) {
                mensaje.append("\n      declarado: ").append(Bytecode.firma(metodo));
                if (!metodo.isAnnotatedWith(anotacion)) {
                    mensaje.append("  (sin @").append(anotacion.getSimpleName()).append(")");
                }
            }
            mensaje.append("\n      se esperaba: ").append(firmaEsperada)
                   .append("  con @").append(anotacion.getSimpleName());
        } else {
            mensaje.append("hay ").append(correctos).append(" métodos ").append(nombreMetodo)
                   .append(" válidos para ").append(para).append("; debe haber exactamente uno");
        }
        mensaje.append(".\n    Código esperado:\n");
        return mensaje.toString();
    }

    /** El nombre en UpperCamelCase que hay tras el prefijo del método, para señalar qué se buscó. */
    private static String eventoDe(String nombreMetodo, String prefijo) {
        return nombreMetodo.startsWith(prefijo) ? "«" + nombreMetodo.substring(prefijo.length()) + "»" : "correspondiente";
    }

    private static PhaseEventManagerFile phaseEventManagerFile(Fase fase) {
        return new PhaseEventManagerFile(pathPhaseEventManager(fase), fase);
    }

    private static Path pathPhaseEventManager(Fase fase) {
        return TiposExpediente.carpeta(fase)
                .resolve(fase.getTipoExpediente().getPhaseEventManagerClassName() + ".java");
    }

    private static String ficheroPhaseEventManager(Fase fase) {
        return TiposExpediente.rel(pathPhaseEventManager(fase));
    }
}
