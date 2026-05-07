package com.educaflow.secretariavirtual.startup;

import com.axelor.app.AppSettings;
import com.axelor.app.AxelorModule;
import com.educaflow.base.infrastructure.mail.MailSender;
import com.educaflow.base.infrastructure.mail.impl.MailSenderImpl;
import com.educaflow.base.infrastructure.mail.impl.SmtpCredentialSimplePassword;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SecretariaVirtualModule extends AxelorModule {

    private final Logger logger = LoggerFactory.getLogger(SecretariaVirtualModule.class);

    @Override
    protected void configure() {
        logger.info("Iniciando Módulo de la Secretaria Virtual...");

        bind(AppEventObserver.class);

        DataBaseStartup.truncateTables();
    }

    @Provides
    @Singleton
    public MailSender provideMailSender() {
        String host = AppSettings.get().get("mail.smtp.host");
        String user = AppSettings.get().get("mail.smtp.user");
        String pass = AppSettings.get().get("mail.smtp.password");

        SmtpCredentialSimplePassword smtpCredentialSimplePassword = new SmtpCredentialSimplePassword(host, user, pass);

        return new MailSenderImpl(smtpCredentialSimplePassword);
    }


}
