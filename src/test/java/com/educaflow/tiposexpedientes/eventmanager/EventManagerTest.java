package com.educaflow.tiposexpedientes.eventmanager;

import com.educaflow.common.buildtools.common.TextUtil;
import com.educaflow.common.buildtools.files.eventmanagerfile.EventManagerFile;
import com.educaflow.common.buildtools.files.tipoexpediente.TipoExpedienteInstanceFile;
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
 * El {@code EventManager} de cada tipo de expediente concuerda con la máquina de estados declarada
 * en su {@code TipoExpedienteInstance.xml}: un método por cada evento y otro por cada estado, ni
 * más ni menos.
 *
 * <p>Estas reglas eran hasta ahora una barrera del build (el {@code check()} con Spoon de
 * {@code createfiles}); viven aquí, sobre bytecode, para que la generación de esqueletos y su
 * validación estén separadas.
 *
 * <p><b>Estos tests se escriben A MANO.</b> No son una proyección de ningún catálogo markdown, al
 * contrario que {@code com.educaflow.architecture} y {@code com.educaflow.views}: este fichero es la
 * fuente de verdad y se edita directamente.
 */
class EventManagerTest {

    private static final String FQCN_EVENT_CONTEXT =
            "com.educaflow.subsystem.expedientes.services.eventmanager.EventContext";
    private static final String FQCN_EVENT_MANAGER =
            "com.educaflow.subsystem.expedientes.services.eventmanager.EventManager";
    private static final String VOID = "void";

    // -----------------------------------------------------------------------------------------
    // E0
    // -----------------------------------------------------------------------------------------

    @Test
    @DisplayName("E0: cada tipo de expediente tiene compilada la clase de su EventManager, que extiende EventManager")
    void e0_existeLaClaseDelEventManager() {
        List<Violacion> violaciones = new ArrayList<>();

        for (TipoExpedienteInstanceFile tipo : TiposExpediente.all()) {
            String fqcn = tipo.getFqcnEventManager();
            Optional<JavaClass> clase = Bytecode.clase(fqcn);

            if (clase.isEmpty()) {
                violaciones.add(new Violacion(tipo.getCode(), ficheroEventManager(tipo),
                        "no existe la clase " + fqcn + " (¿falta compilar, o el fqcnEventManager del XML apunta a otro sitio?)"));
            } else if (!clase.get().isAssignableTo(FQCN_EVENT_MANAGER)) {
                violaciones.add(new Violacion(tipo.getCode(), ficheroEventManager(tipo),
                        "la clase " + fqcn + " no extiende " + FQCN_EVENT_MANAGER));
            }
        }

        Violacion.assertNone("[E0] La clase del EventManager de cada tipo de expediente debe existir y extender "
                + FQCN_EVENT_MANAGER + ".", violaciones);
    }

    // -----------------------------------------------------------------------------------------
    // E1 / E2 — eventos
    // -----------------------------------------------------------------------------------------

    @Test
    @DisplayName("E1: por cada evento hay un método trigger<Evento> con @WhenEvent y la firma correcta")
    void e1_existeUnTriggerPorEvento() {
        List<Violacion> violaciones = new ArrayList<>();

        for (TipoExpedienteInstanceFile tipo : TiposExpediente.all()) {
            Optional<JavaClass> clase = Bytecode.clase(tipo.getFqcnEventManager());
            if (clase.isEmpty()) {
                continue; // ya lo reporta E0; no repetimos el mismo fallo en cada regla
            }
            EventManagerFile eventManagerFile = eventManagerFile(tipo);
            String modelo = eventManagerFile.getModelFQCN();

            for (String evento : TextUtil.getUpperCamelCase(tipo.getEvents())) {
                String nombreMetodo = EventManagerFile.getMethodNameTriggerEvent(evento);
                String esperada = Bytecode.firmaEsperada(nombreMetodo, VOID, modelo, modelo, FQCN_EVENT_CONTEXT);

                List<JavaMethod> homonimos = Bytecode.metodosLlamados(clase.get(), nombreMetodo);
                List<JavaMethod> correctos = homonimos.stream()
                        .filter(m -> m.isAnnotatedWith(WhenEvent.class))
                        .filter(m -> Bytecode.tieneFirma(m, VOID, modelo, modelo, FQCN_EVENT_CONTEXT))
                        .toList();

                if (correctos.size() == 1) {
                    continue;
                }

                violaciones.add(new Violacion(tipo.getCode(), ficheroEventManager(tipo),
                        descripcionMetodoIncorrecto(nombreMetodo, "el evento " + evento, esperada,
                                homonimos, correctos.size(), WhenEvent.class)
                        + eventManagerFile.getSourceCodeTriggerMethod(evento)));
            }
        }

        Violacion.assertNone("[E1] Por cada evento del TipoExpedienteInstance.xml debe haber exactamente un método"
                + " trigger<Evento> anotado @WhenEvent, void y con parámetros (<Entidad>, <Entidad>, EventContext).",
                violaciones);
    }

    @Test
    @DisplayName("E2: no sobra ningún método @WhenEvent que no corresponda a un evento declarado")
    void e2_noSobraNingunTrigger() {
        List<Violacion> violaciones = new ArrayList<>();

        for (TipoExpedienteInstanceFile tipo : TiposExpediente.all()) {
            Optional<JavaClass> clase = Bytecode.clase(tipo.getFqcnEventManager());
            if (clase.isEmpty()) {
                continue;
            }

            Set<String> esperados = new LinkedHashSet<>();
            for (String evento : TextUtil.getUpperCamelCase(tipo.getEvents())) {
                esperados.add(EventManagerFile.getMethodNameTriggerEvent(evento));
            }

            for (JavaMethod metodo : Bytecode.metodosAnotados(clase.get(), WhenEvent.class)) {
                if (!esperados.contains(metodo.getName())) {
                    violaciones.add(new Violacion(tipo.getCode(), ficheroEventManager(tipo),
                            "sobra el método @WhenEvent " + metodo.getName() + "(...): no hay ningún evento "
                            + eventoDe(metodo.getName(), "trigger") + " en el TipoExpedienteInstance.xml."
                            + " Eventos declarados: " + tipo.getEvents()));
                }
            }
        }

        Violacion.assertNone("[E2] Todo método anotado @WhenEvent debe corresponder a un evento declarado en el"
                + " TipoExpedienteInstance.xml.", violaciones);
    }

    // -----------------------------------------------------------------------------------------
    // E3 / E4 — estados
    // -----------------------------------------------------------------------------------------

    @Test
    @DisplayName("E3: por cada estado hay un método onEnter<Estado> con @OnEnterState y la firma correcta")
    void e3_existeUnOnEnterPorEstado() {
        List<Violacion> violaciones = new ArrayList<>();

        for (TipoExpedienteInstanceFile tipo : TiposExpediente.all()) {
            Optional<JavaClass> clase = Bytecode.clase(tipo.getFqcnEventManager());
            if (clase.isEmpty()) {
                continue;
            }
            EventManagerFile eventManagerFile = eventManagerFile(tipo);
            String modelo = eventManagerFile.getModelFQCN();

            for (String estado : TextUtil.getUpperCamelCase(tipo.getStates())) {
                String nombreMetodo = EventManagerFile.getMethodNameOnEnterEvent(estado);
                String esperada = Bytecode.firmaEsperada(nombreMetodo, VOID, modelo, FQCN_EVENT_CONTEXT);

                List<JavaMethod> homonimos = Bytecode.metodosLlamados(clase.get(), nombreMetodo);
                List<JavaMethod> correctos = homonimos.stream()
                        .filter(m -> m.isAnnotatedWith(OnEnterState.class))
                        .filter(m -> Bytecode.tieneFirma(m, VOID, modelo, FQCN_EVENT_CONTEXT))
                        .toList();

                if (correctos.size() == 1) {
                    continue;
                }

                violaciones.add(new Violacion(tipo.getCode(), ficheroEventManager(tipo),
                        descripcionMetodoIncorrecto(nombreMetodo, "el estado " + estado, esperada,
                                homonimos, correctos.size(), OnEnterState.class)
                        + eventManagerFile.getSourceCodeOnEnterMethod(estado)));
            }
        }

        Violacion.assertNone("[E3] Por cada estado del TipoExpedienteInstance.xml debe haber exactamente un método"
                + " onEnter<Estado> anotado @OnEnterState, void y con parámetros (<Entidad>, EventContext).",
                violaciones);
    }

    @Test
    @DisplayName("E4: no sobra ningún método @OnEnterState que no corresponda a un estado declarado")
    void e4_noSobraNingunOnEnter() {
        List<Violacion> violaciones = new ArrayList<>();

        for (TipoExpedienteInstanceFile tipo : TiposExpediente.all()) {
            Optional<JavaClass> clase = Bytecode.clase(tipo.getFqcnEventManager());
            if (clase.isEmpty()) {
                continue;
            }

            Set<String> esperados = new LinkedHashSet<>();
            for (String estado : TextUtil.getUpperCamelCase(tipo.getStates())) {
                esperados.add(EventManagerFile.getMethodNameOnEnterEvent(estado));
            }

            for (JavaMethod metodo : Bytecode.metodosAnotados(clase.get(), OnEnterState.class)) {
                if (!esperados.contains(metodo.getName())) {
                    violaciones.add(new Violacion(tipo.getCode(), ficheroEventManager(tipo),
                            "sobra el método @OnEnterState " + metodo.getName() + "(...): no hay ningún estado "
                            + eventoDe(metodo.getName(), "onEnter") + " en el TipoExpedienteInstance.xml."
                            + " Estados declarados: " + tipo.getStates()));
                }
            }
        }

        Violacion.assertNone("[E4] Todo método anotado @OnEnterState debe corresponder a un estado declarado en el"
                + " TipoExpedienteInstance.xml.", violaciones);
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

    private static EventManagerFile eventManagerFile(TipoExpedienteInstanceFile tipo) {
        Path path = TiposExpediente.carpeta(tipo).resolve(tipo.getEventManagerClassName() + ".java");
        return new EventManagerFile(path, tipo);
    }

    private static String ficheroEventManager(TipoExpedienteInstanceFile tipo) {
        return TiposExpediente.rel(TiposExpediente.carpeta(tipo).resolve(tipo.getEventManagerClassName() + ".java"));
    }
}
