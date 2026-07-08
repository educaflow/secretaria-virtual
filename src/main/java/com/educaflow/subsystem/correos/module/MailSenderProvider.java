package com.educaflow.subsystem.correos.module;

import com.axelor.app.AppSettings;
import com.educaflow.base.infrastructure.mail.MailSender;
import com.educaflow.base.infrastructure.mail.MailSenderFactory;
import com.educaflow.base.infrastructure.mail.impl.SmtpCredentialSimplePassword;
import jakarta.inject.Provider;

public class MailSenderProvider implements Provider<MailSender> {

    @Override
    public MailSender get() {
        AppSettings settings = AppSettings.get();
        SmtpCredentialSimplePassword credencial = new SmtpCredentialSimplePassword(
                settings.get("mail.smtp.host"),
                settings.get("mail.smtp.user"),
                settings.get("mail.smtp.password"));
        return MailSenderFactory.getSmtpMailSender(credencial);
    }

}
