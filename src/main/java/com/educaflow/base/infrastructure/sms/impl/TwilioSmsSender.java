package com.educaflow.base.infrastructure.sms.impl;

import com.educaflow.base.infrastructure.sms.Sms;
import com.educaflow.base.infrastructure.sms.SmsSender;
import com.educaflow.base.infrastructure.sms.TwilioCredential;
import com.educaflow.base.util.TextUtil;
import com.twilio.http.TwilioRestClient;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.Nonnull;

import java.util.Objects;

/**
 * Envía los SMS con la API REST de Twilio.
 *
 * <p>Usa un {@link TwilioRestClient} propio en lugar de {@code Twilio.init(...)}, que guarda las
 * credenciales en un estado estático global de la librería.
 */
public class TwilioSmsSender implements SmsSender {

    private final TwilioRestClient twilioRestClient;
    private final PhoneNumber telefonoOrigen;

    public TwilioSmsSender(@Nonnull TwilioCredential twilioCredential, @Nonnull String telefonoOrigen) {
        Objects.requireNonNull(twilioCredential, "twilioCredential no puede ser null");
        TextUtil.requireNonBlank(telefonoOrigen, "telefonoOrigen no puede ser null ni blank");

        this.telefonoOrigen = new PhoneNumber(telefonoOrigen);
        this.twilioRestClient = new TwilioRestClient.Builder(twilioCredential.accountSid(), twilioCredential.authToken()).build();
    }

    @Override
    public void send(Sms sms) {
        Objects.requireNonNull(sms, "sms no puede ser null");

        Message message = null;
        try {
            //message =Message.creator(new PhoneNumber(sms.telefonoDestino()), telefonoOrigen, sms.mensaje()).create(twilioRestClient);
            message =Message.creator(new PhoneNumber(sms.telefonoDestino()), telefonoOrigen, "sms_marketing_promotions").create(twilioRestClient);
        } catch (Exception ex) {
            throw new RuntimeException("Fallo al enviar SMS mediante Twilio:"+messageToString(message), ex);
        }
    }


    private String messageToString(Message message) {
        if (message!=null) {
            return message.toString();
        } else {
            return "message is null";
        }
    }

}
