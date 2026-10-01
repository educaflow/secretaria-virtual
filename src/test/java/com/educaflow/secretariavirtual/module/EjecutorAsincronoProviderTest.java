package com.educaflow.secretariavirtual.module;

import com.axelor.app.AppSettings;
import com.educaflow.base.infrastructure.async.EjecutorAsincrono;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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

@ExtendWith(MockitoExtension.class)
class EjecutorAsincronoProviderTest {

    @Mock private AppSettings settings;

    private final EjecutorAsincronoProvider provider = new EjecutorAsincronoProvider();

    private MockedStatic<AppSettings> appSettingsStatic;

    private EjecutorAsincrono resultado;

    @BeforeEach
    void setUp() {
        appSettingsStatic = mockStatic(AppSettings.class);
        appSettingsStatic.when(AppSettings::get).thenReturn(settings);
    }

    @AfterEach
    void tearDown() {
        if (resultado != null) {
            resultado.detener();
        }
        appSettingsStatic.close();
    }

    @Test
    void get_conPropiedadConfigurada_usaElTamanoIndicado() {
        when(settings.getInt("async.pool-size", 2)).thenReturn(4);

        resultado = provider.get();

        assertInstanceOf(EjecutorAsincrono.class, resultado);
        verify(settings).getInt("async.pool-size", 2);
    }

    @Test
    void get_sinPropiedadConfigurada_usaDosComoValorPorDefecto() {
        when(settings.getInt("async.pool-size", 2)).thenReturn(2);

        resultado = provider.get();

        assertNotNull(resultado);
        verify(settings).getInt("async.pool-size", 2);
    }
}
