package com.educaflow.apps.expedientes.common;

import com.educaflow.apps.expedientes.common.annotations.OnEnterState;
import com.educaflow.apps.expedientes.common.annotations.WhenEvent;
import com.educaflow.apps.expedientes.db.Expediente;
import com.educaflow.apps.expedientes.db.TipoExpediente;
import com.educaflow.common.util.AxelorViewUtil;
import com.educaflow.common.util.ReflectionUtil;
import com.educaflow.common.validation.messages.BusinessException;
import com.google.common.base.CaseFormat;


import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

public abstract class EventManager<T extends Expediente, State extends Enum<State>, Event extends Enum<Event>, Profile extends Enum<Profile> > {

    final private String VIEW_NAME_STATE_PROFILE_FORMAT ="exp-${EXPEDIENT_CODE}-${STATE_CODE}-${PROFILE_CODE}-form";
    final private String VIEW_NAME_STATE_FORMAT="exp-${EXPEDIENT_CODE}-${STATE_CODE}-form";

    private final Class<T> modelClass;
    private final Class<State> stateClass;
    private final Class<Event> eventClass;
    private final Class<Profile> profileClass;

    public EventManager(Class<T> modelClass, Class<State> stateClass, Class<Event> eventClass, Class<Profile> profileClass) {
        this.modelClass = modelClass;
        this.stateClass = stateClass;
        this.eventClass = eventClass;
        this.profileClass = profileClass;
    }

    public abstract void triggerInitialEvent(T expediente, EventContext<Profile> eventContext) throws BusinessException;


    public void triggerEvent(String strEvent, T expediente, T expedienteOriginal, EventContext<Profile> eventContext) throws BusinessException {
        try {
            Enum event;

            try {
                event = Enum.valueOf(eventClass, strEvent);
            } catch (Exception e) {
                event = Enum.valueOf(CommonEvent.class, strEvent);
            }
            String methodName = "trigger" + CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, event.name());
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

    public void onEnterState(T expediente, EventContext<Profile> eventContext) {
        State state=null;
        try {
            state = (State) Enum.valueOf(stateClass, expediente.getCodeState());
            String methodName = "onEnter" + CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL,state.name());
            Method method = ReflectionUtil.getMethod(this.getClass(), methodName, void.class, OnEnterState.class, new Class<?>[]{modelClass, EventContext.class});

            method.invoke(this, expediente, eventContext);
        } catch (Exception ex) {
            throw new RuntimeException("Error al invocar el state: " + state, ex);
        }
    }

    public String getViewName(T expediente, EventContext<Profile> eventContext) {
        String tipoExpedienteCode=expediente.getTipoExpediente().getCode();
        State state=(State)ReflectionUtil.getEnumConstant(stateClass,expediente.getCodeState());
        Profile profile=eventContext.getProfile();

        String stateProfileViewName=getStateProfileViewName(tipoExpedienteCode, state, profile);
        String stateViewName= getStateViewName(tipoExpedienteCode, state, profile);

        //Los nombre de las vistas tienen una prioridad en caso de que existan varias.
        if (existsView(stateProfileViewName)) {
            return stateProfileViewName;
        }
        if (existsView(stateViewName)) {
            return stateViewName;
        }

        throw new RuntimeException("No existe la vista en el expediente:" + tipoExpedienteCode + " en el contexto:" + profile.name() + " - " + state.name());


    }

    private boolean existsView(String viewName) {
        return AxelorViewUtil.existsView(viewName,"form",this.getModelClass().getName());
    }


    private String getStateProfileViewName(String tipoExpedienteCode, State state, Profile profile) {
        return interpolateViewName(VIEW_NAME_STATE_PROFILE_FORMAT,tipoExpedienteCode, state, profile);
    }
    private String getStateViewName(String tipoExpedienteCode, State state, Profile profile) {
        return interpolateViewName(VIEW_NAME_STATE_FORMAT,tipoExpedienteCode, state, profile);
    }


    private String interpolateViewName(String template,String tipoExpedienteCode, State state, Profile profile) {
        String viewName = template.replace("${EXPEDIENT_CODE}", tipoExpedienteCode)
                .replace("${PROFILE_CODE}", profile.name())
                .replace("${STATE_CODE}", state.name());

        return viewName;
    }

    public Class<T> getModelClass() {
        return modelClass;
    }
    public Class<State> getStateClass() {
        return stateClass;
    }
    public Class<Event> getEventClass() {
        return eventClass;
    }
    public Class<Profile> getProfileClass() {
        return profileClass;
    }

}
