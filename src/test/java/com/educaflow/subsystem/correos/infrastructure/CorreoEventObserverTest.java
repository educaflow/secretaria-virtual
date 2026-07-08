package com.educaflow.subsystem.correos.infrastructure;

import com.axelor.events.ShutdownEvent;
import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CorreoEventObserverTest {

    @Mock private CorreoAsyncExecutor correoAsyncExecutor;

    private CorreoEventObserver observer;

    @BeforeEach
    void setUp() throws Exception {
        observer = new CorreoEventObserver();
        setField(observer, "correoAsyncExecutor", correoAsyncExecutor);
    }

    @Test
    void onAppShutdown_evento_delegaEnDetenerDelExecutor() {
        ShutdownEvent event = mock(ShutdownEvent.class);

        observer.onAppShutdown(event);

        verify(correoAsyncExecutor).detener();
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = CorreoEventObserver.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
