package com.educaflow.base.infrastructure.mail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    void constructor_hostNuloOBlank_lanzaIllegalArgumentException(String host) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new UserPasswordCredential(host, USER_NAME, PASSWORD));

        assertEquals("host no puede ser null ni blank", ex.getMessage());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void constructor_userNameNuloOBlank_lanzaIllegalArgumentException(String userName) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new UserPasswordCredential(HOST, userName, PASSWORD));

        assertEquals("userName no puede ser null ni blank", ex.getMessage());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void constructor_passwordNuloOBlank_lanzaIllegalArgumentException(String password) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new UserPasswordCredential(HOST, USER_NAME, password));

        assertEquals("password no puede ser null ni blank", ex.getMessage());
    }

}
