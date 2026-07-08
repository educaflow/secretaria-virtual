package com.educaflow.subsystem.correos.module;

import com.axelor.app.AppSettings;
import com.educaflow.subsystem.correos.infrastructure.CorreoAsyncExecutor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests de {@link CorreoAsyncExecutorProvider}: {@code Provider} de Guice que construye el
 * {@link CorreoAsyncExecutor} leyendo el tamaño de pool configurado (o su valor por defecto) de
 * {@link AppSettings}.
 *
 * <p>Colaboradores mockeados: el estático {@link AppSettings} y el mock de {@link AppSettings}
 * que devuelve {@code AppSettings.get()}.
 */
@ExtendWith(MockitoExtension.class)
class CorreoAsyncExecutorProviderTest {

    @Mock private AppSettings settingsMock;

    private final CorreoAsyncExecutorProvider provider = new CorreoAsyncExecutorProvider();

    @Test
    void get_conPropiedadConfigurada_usaElTamanoIndicado() {
        try (MockedStatic<AppSettings> appSettingsMock = mockStatic(AppSettings.class)) {
            appSettingsMock.when(AppSettings::get).thenReturn(settingsMock);
            when(settingsMock.getInt("mail.send.pool-size", 2)).thenReturn(4);

            CorreoAsyncExecutor result = provider.get();

            assertNotNull(result);
            assertInstanceOf(CorreoAsyncExecutor.class, result);
        }

        verify(settingsMock).getInt("mail.send.pool-size", 2);
    }

    @Test
    void get_sinPropiedadConfigurada_usaDosComoValorPorDefecto() {
        try (MockedStatic<AppSettings> appSettingsMock = mockStatic(AppSettings.class)) {
            appSettingsMock.when(AppSettings::get).thenReturn(settingsMock);
            when(settingsMock.getInt("mail.send.pool-size", 2)).thenReturn(2);

            CorreoAsyncExecutor result = provider.get();

            assertNotNull(result);
        }

        verify(settingsMock).getInt("mail.send.pool-size", 2);
    }
}
