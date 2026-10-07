package com.educaflow.secretariavirtual.startup;

import com.axelor.events.ShutdownEvent;
import com.axelor.events.StartupEvent;
import com.educaflow.base.infrastructure.async.EjecutorAsincrono;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AppEventObserverTest {

    @Mock
    private EjecutorAsincrono ejecutorAsincrono;

    @Mock
    private CriptografiaStartup criptografiaStartup;

    @Mock
    private ShutdownEvent event;

    @Mock
    private StartupEvent startupEvent;

    @Test
    void onAppStart_falloAlInicializarLaCriptografia_impideArrancar() throws Exception {
        AppEventObserver observer = new AppEventObserver();
        setField(observer, "criptografiaStartup", criptografiaStartup);
        RuntimeException fallo = new RuntimeException("No se encuentra el almacén de certificados confiables");
        doThrow(fallo).when(criptografiaStartup).startup();

        try (MockedStatic<DataBaseStartup> dataBaseStartup = mockStatic(DataBaseStartup.class)) {
            RuntimeException ex = assertThrows(RuntimeException.class, () -> observer.onAppStart(startupEvent));
            assertSame(fallo, ex);
        }
    }

    @Test
    void onAppStart_criptografiaInicializada_arranca() throws Exception {
        AppEventObserver observer = new AppEventObserver();
        setField(observer, "criptografiaStartup", criptografiaStartup);

        try (MockedStatic<DataBaseStartup> dataBaseStartup = mockStatic(DataBaseStartup.class)) {
            observer.onAppStart(startupEvent);
        }

        verify(criptografiaStartup).startup();
    }

    @Test
    void onAppShutdown_evento_detieneElEjecutorAsincrono() throws Exception {
        AppEventObserver observer = new AppEventObserver();
        setField(observer, "ejecutorAsincrono", ejecutorAsincrono);

        observer.onAppShutdown(event);

        verify(ejecutorAsincrono).detener();
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = AppEventObserver.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
