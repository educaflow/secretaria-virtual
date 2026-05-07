package com.educaflow.secretariavirtual.startup;

import com.axelor.app.AppSettings;
import com.axelor.event.Observes;
import com.axelor.events.ShutdownEvent;
import com.axelor.events.StartupEvent;
import com.educaflow.base.infrastructure.mail.MailSender;
import com.educaflow.base.infrastructure.mail.impl.MailSenderImpl;
import com.educaflow.base.infrastructure.mail.impl.SmtpCredentialSimplePassword;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppEventObserver {
    private final Logger logger = LoggerFactory.getLogger(AppEventObserver.class);

    public void onAppStart(@Observes StartupEvent event) {
        logger.info("Iniciando Secretaria Virtual...");

        DataBaseStartup.startup();
        CriptografiaStartup.startup();

    }

    public void onAppShutdown(@Observes ShutdownEvent event) {
        logger.info("Deteniendo Secretaria Virtual...");
    }









}
