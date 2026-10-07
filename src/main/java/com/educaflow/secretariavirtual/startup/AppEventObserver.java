package com.educaflow.secretariavirtual.startup;

import com.axelor.event.Observes;
import com.axelor.events.ShutdownEvent;
import com.axelor.events.StartupEvent;
import com.educaflow.base.infrastructure.async.EjecutorAsincrono;
import com.educaflow.base.infrastructure.mail.MailSender;
import com.educaflow.base.infrastructure.mail.MailSenderFactory;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppEventObserver {
    private final Logger logger = LoggerFactory.getLogger(AppEventObserver.class);

    @Inject
    private EjecutorAsincrono ejecutorAsincrono;

    @Inject
    private CriptografiaStartup criptografiaStartup;

    public void onAppStart(@Observes StartupEvent event) {
        logger.info("Iniciando Secretaria Virtual...");

        DataBaseStartup.startup();
        criptografiaStartup.startup();

    }

    public void onAppShutdown(@Observes ShutdownEvent event) {
        ejecutorAsincrono.detener();
        logger.info("Deteniendo Secretaria Virtual...");
    }









}
