package com.educaflow.base.infrastructure.mail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GMailApiCredentialTest {

    private static final String CLIENT_ID = "client-id";
    private static final String PROJECT_ID = "project-id";
    private static final String CLIENT_SECRET = "client-secret";
    private static final String REFRESH_TOKEN = "refresh-token";

    @Test
    void constructor_todosLosValoresInformados_losConserva() {
        GMailApiCredential credential = new GMailApiCredential(CLIENT_ID, PROJECT_ID, CLIENT_SECRET, REFRESH_TOKEN);

        assertEquals(CLIENT_ID, credential.clientId());
        assertEquals(PROJECT_ID, credential.projectId());
        assertEquals(CLIENT_SECRET, credential.clientSecret());
        assertEquals(REFRESH_TOKEN, credential.refreshToken());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void constructor_clientIdNullOBlank_lanzaIllegalArgument(String valor) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new GMailApiCredential(valor, PROJECT_ID, CLIENT_SECRET, REFRESH_TOKEN));

        assertEquals("clientId no puede ser null ni blank", ex.getMessage());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void constructor_projectIdNullOBlank_lanzaIllegalArgument(String valor) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new GMailApiCredential(CLIENT_ID, valor, CLIENT_SECRET, REFRESH_TOKEN));

        assertEquals("projectId no puede ser null ni blank", ex.getMessage());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void constructor_clientSecretNullOBlank_lanzaIllegalArgument(String valor) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new GMailApiCredential(CLIENT_ID, PROJECT_ID, valor, REFRESH_TOKEN));

        assertEquals("clientSecret no puede ser null ni blank", ex.getMessage());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void constructor_refreshTokenNullOBlank_lanzaIllegalArgument(String valor) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new GMailApiCredential(CLIENT_ID, PROJECT_ID, CLIENT_SECRET, valor));

        assertEquals("refreshToken no puede ser null ni blank", ex.getMessage());
    }
}
