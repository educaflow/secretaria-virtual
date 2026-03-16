package com.educaflow.subsystem.expedientes.services;

import com.educaflow.base.util.ReflectionUtil;

import java.util.ArrayList;
import java.util.List;

public class StateEnum {

    private final Enum state;

    public StateEnum(Enum state) {
        this.state = state;
    }

    public String getCodeState() {
        return state.name();
    }

    public String getProfileCode() {
        return ((Enum<?>) ReflectionUtil.getFieldValue(state, "profile")).name();
    }

    public List<String> getEvents() {
        List<Enum<?>>  events= (List<Enum<?>>)ReflectionUtil.getFieldValue(state, "events");

        ArrayList<String> eventsString= new ArrayList<>(events.size());
        for (Enum<?> event:events) {
            eventsString.add(event.name());
        }

        return eventsString;
    }

    public boolean isInitial() {
        return (Boolean)ReflectionUtil.getFieldValue(state, "initial");
    }

    public boolean isClosed() {
        return (Boolean)ReflectionUtil.getFieldValue(state, "closed");
    }

    public static Enum<?> getInitialState(Class<? extends Enum> stateEnumClass) {
        Enum<?> initialState = null;
        Enum<?>[] states = stateEnumClass.getEnumConstants();

        for (Enum<?> state : states) {
            StateEnum stateEnum = new StateEnum(state);

            if (stateEnum.isInitial()) {
                if (initialState != null) {
                    throw new RuntimeException("Hay más de un estado inicial en la clase: " + stateEnumClass.getName());
                }
                initialState = state;
            }
        }

        if (initialState == null) {
            throw new RuntimeException("No se ha encontrado el estado inicial en la clase: " + stateEnumClass.getName());
        }

        return initialState;
    }

}
