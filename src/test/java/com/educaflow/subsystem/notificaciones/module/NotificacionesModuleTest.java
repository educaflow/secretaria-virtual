package com.educaflow.subsystem.notificaciones.module;

import com.axelor.app.AppSettings;
import com.educaflow.base.infrastructure.mail.MailSender;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificacionesModuleTest {

    private static final String CLAVE_CLIENT_ID = "mail.credentials.gmail.api.clientId";
    private static final String CLAVE_PROJECT_ID = "mail.credentials.gmail.api.projectId";
    private static final String CLAVE_CLIENT_SECRET = "mail.credentials.gmail.api.clientSecret";
    private static final String CLAVE_REFRESH_TOKEN = "mail.credentials.gmail.api.refreshToken";

    private static final String CLAVE_ACCOUNT_SID = "sms.credentials.twilio.accountSid";
    private static final String CLAVE_AUTH_TOKEN = "sms.credentials.twilio.authToken";
    private static final String CLAVE_FROM = "sms.twilio.from";

    private static final String CLIENT_ID = "client-id-de-prueba";
    private static final String PROJECT_ID = "project-id-de-prueba";
    private static final String CLIENT_SECRET = "client-secret-de-prueba";
    private static final String REFRESH_TOKEN = "refresh-token-de-prueba";

    private static final String ACCOUNT_SID = "AC0000000000000000000000000000000";
    private static final String AUTH_TOKEN = "auth-token-de-prueba";
    private static final String FROM = "+34600999888";

    @Mock
    private AppSettings settings;

    private final NotificacionesModule module = new NotificacionesModule();

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

    /*************************************** mailSender ***************************************/

    @Test
    void mailSender_credencialesGmailConfiguradas_devuelveElEmisor() {
        when(settings.get(CLAVE_CLIENT_ID)).thenReturn(CLIENT_ID);
        when(settings.get(CLAVE_PROJECT_ID)).thenReturn(PROJECT_ID);
        when(settings.get(CLAVE_CLIENT_SECRET)).thenReturn(CLIENT_SECRET);
        when(settings.get(CLAVE_REFRESH_TOKEN)).thenReturn(REFRESH_TOKEN);

        MailSender resultado = module.mailSender();

        assertNotNull(resultado);
    }

    @Test
    void mailSender_clientIdEnBlanco_lanzaIllegalArgumentException() {
        when(settings.get(CLAVE_CLIENT_ID)).thenReturn("");
        lenient().when(settings.get(CLAVE_PROJECT_ID)).thenReturn(PROJECT_ID);
        lenient().when(settings.get(CLAVE_CLIENT_SECRET)).thenReturn(CLIENT_SECRET);
        lenient().when(settings.get(CLAVE_REFRESH_TOKEN)).thenReturn(REFRESH_TOKEN);

        assertThrows(IllegalArgumentException.class, module::mailSender);
    }

    @Test
    void mailSender_refreshTokenNoConfigurado_lanzaNullPointerException() {
        when(settings.get(CLAVE_CLIENT_ID)).thenReturn(CLIENT_ID);
        when(settings.get(CLAVE_PROJECT_ID)).thenReturn(PROJECT_ID);
        when(settings.get(CLAVE_CLIENT_SECRET)).thenReturn(CLIENT_SECRET);
        when(settings.get(CLAVE_REFRESH_TOKEN)).thenReturn(null);

        assertThrows(NullPointerException.class, module::mailSender);
    }

    /*************************************** smsSender ***************************************/

    @Test
    void smsSender_credencialesYEmisorConfigurados_devuelveElEmisor() {
        when(settings.get(CLAVE_ACCOUNT_SID)).thenReturn(ACCOUNT_SID);
        when(settings.get(CLAVE_AUTH_TOKEN)).thenReturn(AUTH_TOKEN);
        when(settings.get(CLAVE_FROM)).thenReturn(FROM);

        SmsSender resultado = module.smsSender();

        assertNotNull(resultado);
    }

    @Test
    void smsSender_sinAccountSid_lanzaIllegalArgumentException() {
        when(settings.get(CLAVE_ACCOUNT_SID)).thenReturn("");
        when(settings.get(CLAVE_AUTH_TOKEN)).thenReturn(AUTH_TOKEN);
        // La credencial se construye antes de leer el número emisor: si falla, ese stub no se llega a usar.
        lenient().when(settings.get(CLAVE_FROM)).thenReturn(FROM);

        assertThrows(IllegalArgumentException.class, module::smsSender);
    }

    @Test
    void smsSender_sinAuthToken_lanzaNullPointerException() {
        when(settings.get(CLAVE_ACCOUNT_SID)).thenReturn(ACCOUNT_SID);
        when(settings.get(CLAVE_AUTH_TOKEN)).thenReturn(null);
        lenient().when(settings.get(CLAVE_FROM)).thenReturn(FROM);

        assertThrows(NullPointerException.class, module::smsSender);
    }

    @Test
    void smsSender_sinNumeroEmisor_lanzaIllegalArgumentException() {
        when(settings.get(CLAVE_ACCOUNT_SID)).thenReturn(ACCOUNT_SID);
        when(settings.get(CLAVE_AUTH_TOKEN)).thenReturn(AUTH_TOKEN);
        when(settings.get(CLAVE_FROM)).thenReturn("");

        assertThrows(IllegalArgumentException.class, module::smsSender);
    }
}
