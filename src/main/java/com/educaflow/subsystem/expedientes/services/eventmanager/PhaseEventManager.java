package com.educaflow.subsystem.expedientes.services.eventmanager;

import com.educaflow.base.util.AxelorUtil;
import com.educaflow.subsystem.expedientes.db.Expediente;
import com.educaflow.subsystem.expedientes.db.Profile;
import com.educaflow.base.util.ReflectionUtil;
import com.educaflow.base.infrastructure.validation.messages.BusinessException;
import com.google.common.base.CaseFormat;


import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Atiende los eventos y las entradas en estado de <b>una fase</b> de un tipo de expediente: hay un
 * {@code PhaseEventManagerImpl} por fase, en el paquete de la fase.
 *
 * <p>El <b>evento inicial</b> no vive aquí: es del tipo de expediente entero (se dispara cuando
 * todavía no hay estado del que partir) y lo atiende un {@link InitialEventManager}, uno solo por
 * tipo, en el paquete base de la versión.
 */
public abstract class PhaseEventManager<T extends Expediente> {

    final private String VIEW_NAME_STATE_PROFILE_FORMAT ="exp-${EXPEDIENT_CODE}-${PHASE_CODE}-${STATE_CODE}-${PROFILE_CODE}-form";
    final private String VIEW_NAME_STATE_FORMAT="exp-${EXPEDIENT_CODE}-${PHASE_CODE}-${STATE_CODE}-form";

    private final Class<T> modelClass;

    public PhaseEventManager(Class<T> modelClass) {
        this.modelClass = modelClass;
    }

    /**
     * Invoca el {@code trigger<Evento>} del evento que se dispara.
     *
     * <p>El nombre del método sale del propio string: los eventos no son un enum, así que no hay
     * nada que resolver antes. Que el evento sea disparable desde el estado en el que está el
     * expediente lo ha comprobado ya {@code Tramitador} contra {@code State.getEvents()}.
     */
    public void triggerEvent(String strEvent, T expediente, T expedienteOriginal, EventContext eventContext) throws BusinessException {
        try {
            String methodName = "trigger" + CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, strEvent);
            Method method = ReflectionUtil.getMethod(this.getClass(), methodName, void.class, WhenEvent.class, new Class<?>[]{modelClass, modelClass, EventContext.class});

            method.invoke(this, expediente, expedienteOriginal, eventContext);
        } catch (InvocationTargetException ex) {
            if (ex.getTargetException() instanceof BusinessException) {
                throw (BusinessException)ex.getTargetException();
            } else {
                throw new RuntimeException("Error al invocar el event: " + strEvent, ex);
            }
        } catch (Exception ex) {
            throw new RuntimeException("Error al invocar el event: " + strEvent , ex);
        }
    }

    /**
     * Invoca el {@code onEnter<Estado>} del estado en el que está el expediente.
     *
     * <p>El nombre del método sale del {@code codeState}, que es el código del estado dentro de su
     * fase. Quien llama debe haber resuelto antes, con {@code ExpedienteLocator}, el PhaseEventManager de
     * la fase del estado <b>actual</b>: en una transición que cruza fases, la clase que atendió el
     * evento no es la que tiene este método.
     */
    public void onEnterState(T expediente, EventContext eventContext) {
        String codeState = expediente.getCodeState();
        try {
            String methodName = "onEnter" + CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, codeState);
            Method method = ReflectionUtil.getMethod(this.getClass(), methodName, void.class, OnEnterState.class,
                    new Class<?>[]{modelClass, EventContext.class});

            method.invoke(this, expediente, eventContext);
        } catch (Exception ex) {
            throw new RuntimeException("Error al invocar el state: " + codeState, ex);
        }
    }

    /**
     * El nombre de la vista del estado actual. Los nombres de vista de Axelor son globales, así que
     * llevan la fase y el estado como dos segmentos: el código de un estado solo es único dentro de
     * su fase. Es el mismo nombre que compone el viewprocessor al preprocesar el {@code views.xml}
     * de cada fase, y por eso las vistas no necesitan localizador.
     */
    public String getViewName(T expediente, EventContext eventContext) {
        String tipoExpedienteCode=expediente.getTipoExpediente().getCode();
        Profile profile=eventContext.getProfile();

        //Hueco simétrico al guard de codePhase de ExpedienteLocator: una fila con la fase puesta y el
        //estado nulo lo pasaría y reventaría aquí con un NPE pelado al interpolar el nombre de vista.
        if ((expediente.getCodeState() == null) || (expediente.getCodeState().isBlank())) {
            throw new RuntimeException("El expediente no tiene estado (codeState) para el tipo de"
                    + " expediente " + tipoExpedienteCode + ". La pareja (codePhase, codeState) la"
                    + " escribe ExpedienteUtil.updateState; si está vacía, la fila se ha creado o"
                    + " modificado por fuera de la tramitación.");
        }

        String stateProfileViewName=interpolateViewName(VIEW_NAME_STATE_PROFILE_FORMAT, tipoExpedienteCode, expediente, profile);
        String stateViewName=interpolateViewName(VIEW_NAME_STATE_FORMAT, tipoExpedienteCode, expediente, profile);

        //Los nombre de las vistas tienen una prioridad en caso de que existan varias.
        if (existsView(stateProfileViewName)) {
            return stateProfileViewName;
        }
        if (existsView(stateViewName)) {
            return stateViewName;
        }

        throw new RuntimeException("No existe la vista en el expediente:" + tipoExpedienteCode + " en el contexto:"
                + profile.name() + " - " + expediente.getCodePhase() + "/" + expediente.getCodeState());


    }

    private boolean existsView(String viewName) {
        return AxelorUtil.existsView(viewName,"form",this.getModelClass().getName());
    }


    private String interpolateViewName(String template, String tipoExpedienteCode, Expediente expediente, Profile profile) {
        return template.replace("${EXPEDIENT_CODE}", tipoExpedienteCode)
                .replace("${PROFILE_CODE}", profile.name())
                .replace("${PHASE_CODE}", expediente.getCodePhase())
                .replace("${STATE_CODE}", expediente.getCodeState());
    }

    public Class<T> getModelClass() {
        return modelClass;
    }

}
