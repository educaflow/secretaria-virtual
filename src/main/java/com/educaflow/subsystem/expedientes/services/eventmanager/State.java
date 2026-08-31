package com.educaflow.subsystem.expedientes.services.eventmanager;

import com.educaflow.subsystem.expedientes.db.Profile;

import java.util.Set;

/**
 * Un estado de un tipo de expediente. La implementa el enum público de su fase dentro de la clase
 * {@code States} generada, de modo que cada estado es un singleton comparable con {@code ==}.
 *
 * <p>La identidad de un estado es la pareja (fase, código): el código solo es único dentro de su
 * fase, y por eso lo que se persiste son las dos columnas {@code codePhase} y {@code codeState}.
 */
public interface State {

    Phase getPhase();

    /** El {@code name} del XML, en UPPER_SNAKE_CASE. Es lo que se persiste en {@code codeState}. */
    String getCode();

    /** El {@code title} del XML, o el {@code name} humanizado si no lo hay. Va a {@code nameState}. */
    String getName();

    /** El perfil que ve/opera el estado, o {@code null} si el estado no declara ninguno. */
    Profile getProfile();

    /** Los eventos disparables desde el estado, en orden de declaración. Conjunto inmutable. */
    Set<String> getEvents();

    boolean isInitial();

    /** El {@code closed="true"} del XML: el expediente queda cerrado al entrar aquí. */
    boolean isFinal();
}
