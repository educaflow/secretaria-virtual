package com.educaflow.secretariavirtual.startup;

import com.axelor.events.ShutdownEvent;
import com.educaflow.base.infrastructure.async.EjecutorAsincrono;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AppEventObserverTest {

    @Mock
    private EjecutorAsincrono ejecutorAsincrono;

    @Mock
    private ShutdownEvent event;

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
