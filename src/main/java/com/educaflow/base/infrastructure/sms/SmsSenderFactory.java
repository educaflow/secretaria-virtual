package com.educaflow.base.infrastructure.sms;

import com.educaflow.base.infrastructure.sms.impl.TwilioSmsSender;
import com.educaflow.base.util.TextUtil;

import java.util.Objects;

public class SmsSenderFactory {

    public static SmsSender getTwilioSmsSender(TwilioCredential twilioCredential, String telefonoOrigen) {
        Objects.requireNonNull(twilioCredential, "twilioCredential no puede ser null");
        TextUtil.requireNonBlank(telefonoOrigen, "telefonoOrigen no puede ser null ni blank");
        return new TwilioSmsSender(twilioCredential, telefonoOrigen);
    }

}
