package com.educaflow.tiposexpedientes.phaseeventmanager;

import com.educaflow.common.buildtools.files.phaseeventmanagerfile.PhaseEventManagerFile;
import com.educaflow.common.buildtools.files.stateeventvalidator.StateEventValidatorFile;
import com.educaflow.common.buildtools.files.tipoexpediente.Fase;
import com.educaflow.common.buildtools.files.tipoexpediente.State;
import com.educaflow.subsystem.expedientes.services.eventmanager.PhaseEventManager;
import com.educaflow.subsystem.expedientes.services.eventmanager.InitialEventManager;
import com.educaflow.subsystem.expedientes.services.validation.StateEventValidator;
import com.educaflow.tiposexpedientes.support.TiposExpediente;
import com.educaflow.tiposexpedientes.support.Violacion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Ningún nombre de método que el build compone a partir del {@code TipoExpedienteInstance.xml}
 * ({@code onEnter<Estado>}, {@code trigger<Evento>}, {@code getForState<Estado>InEvent<Evento>})
 * puede pisar un método público de las clases base {@code PhaseEventManager} o
 * {@code StateEventValidator}, ni el {@code triggerInitialEvent} de {@code InitialEventManager}.
 *
 * <p>Es la única parte de la regla 8 que el generador no puede comprobar: corre <b>antes</b> de
 * compilar {@code secretaria-virtual} y los build-tools no comparten código con el runtime, así que
 * allí esa lista solo podría existir duplicada a mano y haría falta otro test para vigilar la copia.
 * Aquí, en cambio, están a la vez el XML y las clases base reales en el classpath, y la lista se
 * deriva por reflexión sin duplicar nada: si la base gana, pierde o renombra un método público, el
 * test se ajusta solo.
 *
 * <p>El caso real que motiva la regla: un estado llamado {@code STATE}. Su
 * {@code onEnterState(<Modelo>, EventContext)} tiene <b>exactamente</b> la firma del dispatcher
 * {@code PhaseEventManager.onEnterState}, de modo que lo sobrescribiría y el despacho ejecutaría el
 * handler de ese estado para <b>cualquier</b> estado de la fase, sin error de compilación ni de
 * runtime.
 *
 * <p>Se compara solo el <b>nombre</b>, sin firmas, a propósito: así se prohíben también las
 * sobrecargas legales pero confusas y no hay que mantener aridades sincronizadas con la base.
 *
 * <p><b>{@code InitialEventManager} cuenta como clase base a estos efectos, aunque el
 * {@code PhaseEventManagerImpl} no lo implemente.</b> Al sacar {@code triggerInitialEvent} de
 * {@code PhaseEventManager}, ese nombre dejó de estar en la API de la base del {@code PhaseEventManagerImpl},
 * de modo que un evento llamado {@code INITIAL_EVENT} habría pasado a ser legal aquí. No lo es, y
 * seguir reservándolo es lo correcto: su {@code trigger} entraría en el {@code PhaseEventManagerImpl} de
 * la fase, donde la regla E5 de {@code PhaseEventManagerTest} prohíbe cualquier
 * {@code triggerInitialEvent} —porque un método así ya no lo llama nadie—, y el tipo de expediente
 * quedaría atrapado entre dos reglas que se contradicen, con dos mensajes de error que no explican
 * lo que pasa. Además el nombre seguiría siendo, para quien lo lee, el del evento inicial.
 *
 * <p>La reserva no se escribe a mano: se deriva por reflexión de {@code InitialEventManager},
 * igual que las otras dos, de modo que si esa interfaz renombra su método la lista se ajusta sola.
 *
 * <p><b>Estos tests se escriben A MANO.</b> Este fichero es la fuente de verdad y se edita
 * directamente.
 */
class ApiBaseReservadaTest {

    @Test
    @DisplayName("A1: ningún nombre de método generado a partir del XML pisa la API pública de las clases base")
    void a1_ningunNombreGeneradoPisaLaApiBase() {
        Set<String> apiBase = new HashSet<>();
        for (Method m : PhaseEventManager.class.getMethods())        { apiBase.add(m.getName()); }
        for (Method m : StateEventValidator.class.getMethods()) { apiBase.add(m.getName()); }
        for (Method m : InitialEventManager.class.getMethods()) { apiBase.add(m.getName()); }

        List<Violacion> violaciones = new ArrayList<>();

        for (Fase fase : TiposExpediente.todasLasFases()) {
            for (State state : fase.getStates()) {
                comprobar(PhaseEventManagerFile.getMethodNameOnEnterEvent(state.getNameUpperCamelCase()),
                        "el estado '" + state.getName() + "'", apiBase, fase, violaciones);

                if (state.getEvents() == null) {
                    continue;
                }
                for (String evento : state.getEventsUpperCamelCase()) {
                    comprobar(StateEventValidatorFile.getMethodNameBeanValidationRules(
                                    state.getNameUpperCamelCase(), evento),
                            "la pareja estado '" + state.getName() + "' / evento '" + evento + "'",
                            apiBase, fase, violaciones);
                }
            }

            for (String evento : fase.getEventsUpperCamelCase()) {
                comprobar(PhaseEventManagerFile.getMethodNameTriggerEvent(evento),
                        "el evento '" + evento + "'", apiBase, fase, violaciones);
            }
        }

        Violacion.assertNone("[A1] Ningún nombre de método compuesto a partir de un estado o un evento del"
                + " TipoExpedienteInstance.xml puede coincidir con un método público de " + PhaseEventManager.class.getName()
                + ", de " + StateEventValidator.class.getName() + " o de " + InitialEventManager.class.getName()
                + ": lo sobrescribiría en silencio, o chocaría con el método que atiende el evento inicial.",
                violaciones);
    }

    private static void comprobar(String metodo, String procedencia, Set<String> apiBase, Fase fase, List<Violacion> violaciones) {
        if (apiBase.contains(metodo) == false) {
            return;
        }

        violaciones.add(new Violacion(TiposExpediente.nombre(fase),
                TiposExpediente.rel(fase.getTipoExpediente().getPath()),
                procedencia + " produce el método '" + metodo + "', que es un método público de las clases base"
                + " o el del evento inicial. Renombra el estado o el evento en el TipoExpedienteInstance.xml."));
    }
}
