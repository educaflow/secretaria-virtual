package com.educaflow.secretariavirtual.startup;

import com.axelor.event.Observes;
import com.axelor.events.ShutdownEvent;
import com.axelor.events.StartupEvent;
import com.educaflow.base.infrastructure.mail.MailSender;
import com.educaflow.base.infrastructure.mail.MailSenderFactory;
import com.educaflow.subsystem.correos.service.CorreoService;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppEventObserver {
    private final Logger logger = LoggerFactory.getLogger(AppEventObserver.class);

    public void onAppStart(@Observes StartupEvent event) {
        logger.info("Iniciando Secretaria Virtual...");

        DataBaseStartup.startup();


        try {
            CriptografiaStartup.startup();
        } catch (Exception ex) {
            logger.error("Falló al inicializar la criptografía",ex);
        }

    }

    public void onAppShutdown(@Observes ShutdownEvent event) {
        logger.info("Deteniendo Secretaria Virtual...");
    }









}
