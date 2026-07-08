package com.educaflow.subsystem.correos.infrastructure;

import com.axelor.event.Observes;
import com.axelor.events.ShutdownEvent;
import com.axelor.events.StartupEvent;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CorreoEventObserver {

    private static final Logger log = LoggerFactory.getLogger(CorreoEventObserver.class);

    @Inject
    private CorreoAsyncExecutor correoAsyncExecutor;

    public void onAppStart(@Observes StartupEvent event) {
        log.info("Executor de envío de correos listo");
    }

    public void onAppShutdown(@Observes ShutdownEvent event) {
        correoAsyncExecutor.detener();
    }
}
