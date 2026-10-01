package com.educaflow.subsystem.sms.module;

import com.axelor.app.AppSettings;
import com.educaflow.base.infrastructure.sms.SmsSender;
import com.educaflow.base.infrastructure.sms.SmsSenderFactory;
import com.educaflow.base.infrastructure.sms.TwilioCredential;
import jakarta.inject.Provider;

public class SmsSenderProvider implements Provider<SmsSender> {

    @Override
    public SmsSender get() {
        AppSettings settings = AppSettings.get();
        TwilioCredential credencial = new TwilioCredential(
                settings.get("sms.credentials.twilio.accountSid"),
                settings.get("sms.credentials.twilio.authToken"));
        return SmsSenderFactory.getTwilioSmsSender(credencial, settings.get("sms.twilio.from"));
    }

}
