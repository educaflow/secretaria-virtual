package com.educaflow.subsystem.correos.module;

import com.axelor.app.AppSettings;
import com.educaflow.base.infrastructure.mail.GMailApiCredential;
import com.educaflow.base.infrastructure.mail.MailSender;
import com.educaflow.base.infrastructure.mail.MailSenderFactory;
import jakarta.inject.Provider;

public class MailSenderProvider implements Provider<MailSender> {

    @Override
    public MailSender get() {
        AppSettings settings = AppSettings.get();
        GMailApiCredential credencial = new GMailApiCredential(
                settings.get("mail.credentials.gmail.api.clientId"),
                settings.get("mail.credentials.gmail.api.projectId"),
                settings.get("mail.credentials.gmail.api.clientSecret"),
                settings.get("mail.credentials.gmail.api.refreshToken"));
        return MailSenderFactory.getGMailApiMailSender(credencial);
    }

}
