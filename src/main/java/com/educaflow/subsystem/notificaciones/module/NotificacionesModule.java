package com.educaflow.subsystem.notificaciones.module;

import com.axelor.app.AppSettings;
import com.axelor.app.AxelorModule;
import com.educaflow.base.infrastructure.mail.GMailApiCredential;
import com.educaflow.base.infrastructure.mail.MailSender;
import com.educaflow.base.infrastructure.mail.MailSenderFactory;
import com.educaflow.base.infrastructure.sms.SmsSender;
import com.educaflow.base.infrastructure.sms.SmsSenderFactory;
import com.educaflow.base.infrastructure.sms.TwilioCredential;
import com.google.inject.Provides;

public class NotificacionesModule extends AxelorModule {

    @Override
    protected void configure() {
    }

    @Provides
    MailSender mailSender() {
        AppSettings settings = AppSettings.get();
        GMailApiCredential credencial = new GMailApiCredential(
                settings.get("mail.credentials.gmail.api.clientId"),
                settings.get("mail.credentials.gmail.api.projectId"),
                settings.get("mail.credentials.gmail.api.clientSecret"),
                settings.get("mail.credentials.gmail.api.refreshToken"));
        return MailSenderFactory.getGMailApiMailSender(credencial);
    }

    @Provides
    SmsSender smsSender() {
        AppSettings settings = AppSettings.get();
        TwilioCredential credencial = new TwilioCredential(
                settings.get("sms.credentials.twilio.accountSid"),
                settings.get("sms.credentials.twilio.authToken"));
        return SmsSenderFactory.getTwilioSmsSender(credencial, settings.get("sms.twilio.from"));
    }

}
