package com.educaflow.subsystem.sms.module;

import com.axelor.app.AppSettings;
import com.educaflow.base.infrastructure.sms.SmsSender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmsSenderProviderTest {

    private static final String CLAVE_ACCOUNT_SID = "sms.credentials.twilio.accountSid";
    private static final String CLAVE_AUTH_TOKEN = "sms.credentials.twilio.authToken";
    private static final String CLAVE_FROM = "sms.twilio.from";

    private static final String ACCOUNT_SID = "AC0000000000000000000000000000000";
    private static final String AUTH_TOKEN = "token-de-prueba";
    private static final String FROM = "+34600999888";

    @Mock private AppSettings settings;

    private final SmsSenderProvider provider = new SmsSenderProvider();

    private MockedStatic<AppSettings> appSettingsStatic;

    @BeforeEach
    void setUp() {
        appSettingsStatic = mockStatic(AppSettings.class);
        appSettingsStatic.when(AppSettings::get).thenReturn(settings);
    }

    @AfterEach
    void tearDown() {
        appSettingsStatic.close();
    }

    @Test
    void get_conCredencialesYNumeroEmisorConfigurados_devuelveElEmisor() {
        when(settings.get(CLAVE_ACCOUNT_SID)).thenReturn(ACCOUNT_SID);
        when(settings.get(CLAVE_AUTH_TOKEN)).thenReturn(AUTH_TOKEN);
        when(settings.get(CLAVE_FROM)).thenReturn(FROM);

        SmsSender resultado = provider.get();

        assertNotNull(resultado);
        verify(settings).get(CLAVE_ACCOUNT_SID);
        verify(settings).get(CLAVE_AUTH_TOKEN);
        verify(settings).get(CLAVE_FROM);
    }

    @Test
    void get_sinAccountSidConfigurado_lanzaIllegalArgumentException() {
        when(settings.get(CLAVE_ACCOUNT_SID)).thenReturn("");
        when(settings.get(CLAVE_AUTH_TOKEN)).thenReturn(AUTH_TOKEN);
        // La credencial se valida antes de leer el número emisor, así que este stub no llega a usarse.
        lenient().when(settings.get(CLAVE_FROM)).thenReturn(FROM);

        assertThrows(IllegalArgumentException.class, provider::get);
    }

    @Test
    void get_sinAuthTokenConfigurado_lanzaExcepcion() {
        when(settings.get(CLAVE_ACCOUNT_SID)).thenReturn(ACCOUNT_SID);
        when(settings.get(CLAVE_AUTH_TOKEN)).thenReturn(null);
        lenient().when(settings.get(CLAVE_FROM)).thenReturn(FROM);

        assertThrows(NullPointerException.class, provider::get);
    }

    @Test
    void get_sinNumeroEmisorConfigurado_lanzaIllegalArgumentException() {
        when(settings.get(CLAVE_ACCOUNT_SID)).thenReturn(ACCOUNT_SID);
        when(settings.get(CLAVE_AUTH_TOKEN)).thenReturn(AUTH_TOKEN);
        when(settings.get(CLAVE_FROM)).thenReturn("");

        assertThrows(IllegalArgumentException.class, provider::get);
    }
}
