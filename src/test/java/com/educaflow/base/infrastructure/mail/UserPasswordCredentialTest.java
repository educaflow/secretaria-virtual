package com.educaflow.base.infrastructure.mail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserPasswordCredentialTest {

    private static final String HOST = "smtp.example.com";
    private static final String USER_NAME = "usuario";
    private static final String PASSWORD = "secreto";

    @Test
    void constructor_todosLosValoresInformados_losConserva() {
        UserPasswordCredential credential = new UserPasswordCredential(HOST, USER_NAME, PASSWORD);

        assertEquals(HOST, credential.host());
        assertEquals(USER_NAME, credential.userName());
        assertEquals(PASSWORD, credential.password());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void constructor_hostNuloOBlank_lanzaExcepcion(String host) {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> new UserPasswordCredential(host, USER_NAME, PASSWORD));

        assertEquals("host no puede ser null ni blank", ex.getMessage());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void constructor_userNameNuloOBlank_lanzaExcepcion(String userName) {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> new UserPasswordCredential(HOST, userName, PASSWORD));

        assertEquals("userName no puede ser null ni blank", ex.getMessage());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void constructor_passwordNuloOBlank_lanzaExcepcion(String password) {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> new UserPasswordCredential(HOST, USER_NAME, password));

        assertEquals("password no puede ser null ni blank", ex.getMessage());
    }

    @Test
    void toString_noMuestraLaPassword() {
        assertFalse(new UserPasswordCredential(HOST, USER_NAME, PASSWORD).toString().contains(PASSWORD));
    }
}
