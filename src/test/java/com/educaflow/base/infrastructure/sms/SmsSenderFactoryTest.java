package com.educaflow.base.infrastructure.sms;

import com.educaflow.base.infrastructure.sms.impl.TwilioSmsSender;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SmsSenderFactoryTest {

    @Test
    void getTwilioSmsSender_conCredencial_devuelveTwilioSmsSender() {
        SmsSender smsSender = SmsSenderFactory.getTwilioSmsSender(new TwilioCredential("ACxxxx", "token"), "+34600000000");

        assertInstanceOf(TwilioSmsSender.class, smsSender);
    }

    @Test
    void getTwilioSmsSender_conCredencialNull_lanzaExcepcion() {
        assertThrows(NullPointerException.class, () -> SmsSenderFactory.getTwilioSmsSender(null, "+34600000000"));
    }

    @Test
    void getTwilioSmsSender_conFromNullOBlank_lanzaExcepcion() {
        TwilioCredential credential = new TwilioCredential("ACxxxx", "token");

        assertThrows(NullPointerException.class, () -> SmsSenderFactory.getTwilioSmsSender(credential, null));
        assertThrows(IllegalArgumentException.class, () -> SmsSenderFactory.getTwilioSmsSender(credential, ""));
        assertThrows(IllegalArgumentException.class, () -> SmsSenderFactory.getTwilioSmsSender(credential, " "));
    }

    @Test
    void send_conSmsNull_lanzaExcepcion() {
        SmsSender smsSender = SmsSenderFactory.getTwilioSmsSender(new TwilioCredential("ACxxxx", "token"), "+34600000000");

        assertThrows(NullPointerException.class, () -> smsSender.send(null));
    }
}
