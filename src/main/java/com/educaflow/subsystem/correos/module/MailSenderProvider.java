package com.educaflow.subsystem.correos.module;

import com.axelor.app.AppSettings;
import com.educaflow.base.infrastructure.mail.MailSender;
import com.educaflow.base.infrastructure.mail.impl.MailSenderImpl;
import com.educaflow.base.infrastructure.mail.impl.SmtpCredentialSimplePassword;
import jakarta.inject.Provider;

/**
 * Único punto que lee las propiedades {@code mail.smtp.*} de {@link AppSettings} (invariante G-001)
 * y construye el {@link MailSender} con las credenciales SMTP.
 */
public class MailSenderProvider implements Provider<MailSender> {

    @Override
    public MailSender get() {
        AppSettings settings = AppSettings.get();
        String host = settings.get("mail.smtp.host");
        String userName = settings.get("mail.smtp.user");
        String password = settings.get("mail.smtp.password");

        SmtpCredentialSimplePassword credential = new SmtpCredentialSimplePassword(host, userName, password);

        return new MailSenderImpl(credential);
    }
}