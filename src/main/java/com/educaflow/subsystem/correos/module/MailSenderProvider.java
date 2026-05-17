package com.educaflow.subsystem.correos.module;

import com.axelor.app.AppSettings;
import com.educaflow.base.infrastructure.mail.MailSender;
import com.educaflow.base.infrastructure.mail.impl.MailSenderImpl;
import com.educaflow.base.infrastructure.mail.impl.SmtpCredentialSimplePassword;
import com.google.inject.Provider;

public class MailSenderProvider implements Provider<MailSender> {

    @Override
    public MailSender get() {
        AppSettings settings = AppSettings.get();
        String host = settings.get("mail.smtp.host");
        String user = settings.get("mail.smtp.user");
        String password = settings.get("mail.smtp.password");

        SmtpCredentialSimplePassword credential = new SmtpCredentialSimplePassword(host, user, password);
        return new MailSenderImpl(credential);
    }
}
