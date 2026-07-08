package com.educaflow.subsystem.correos.module;

import com.axelor.app.AppSettings;
import com.educaflow.base.infrastructure.mail.MailSender;
import com.educaflow.base.infrastructure.mail.impl.MailSenderImplSmtp;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests de {@link MailSenderProvider}: {@code Provider} de Guice que construye el {@link MailSender}
 * real leyendo las credenciales SMTP de {@link AppSettings}.
 *
 * <p>Colaboradores mockeados: el estático {@link AppSettings} y el mock de {@link AppSettings}
 * que devuelve {@code AppSettings.get()}.
 */
@ExtendWith(MockitoExtension.class)
class MailSenderProviderTest {

    @Mock private AppSettings settingsMock;

    private final MailSenderProvider provider = new MailSenderProvider();

    @Test
    void get_leeCredencialesDeAppSettingsYDevuelveMailSenderImpl() {
        try (MockedStatic<AppSettings> appSettingsMock = mockStatic(AppSettings.class)) {
            appSettingsMock.when(AppSettings::get).thenReturn(settingsMock);
            when(settingsMock.get("mail.smtp.host")).thenReturn("smtp.test.com");
            when(settingsMock.get("mail.smtp.user")).thenReturn("user@test.com");
            when(settingsMock.get("mail.smtp.password")).thenReturn("secret");

            MailSender result = provider.get();

            assertInstanceOf(MailSenderImplSmtp.class, result);
        }

        verify(settingsMock).get("mail.smtp.host");
        verify(settingsMock).get("mail.smtp.user");
        verify(settingsMock).get("mail.smtp.password");
    }
}
