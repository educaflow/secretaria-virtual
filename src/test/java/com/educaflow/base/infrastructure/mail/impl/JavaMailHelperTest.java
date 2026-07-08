package com.educaflow.base.infrastructure.mail.impl;

import com.educaflow.base.infrastructure.mail.Mail;
import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.Session;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class JavaMailHelperTest {

    private static final Session SESSION = Session.getDefaultInstance(new Properties());

    @Test
    void getMessage_conCcNoVacio_estableceCabeceraCc() throws Exception {
        Mail mail = new Mail(
                List.of("destinatario@x.com"),
                List.of("copia@x.com"),
                List.of(),
                "remitente@x.com",
                "Asunto de prueba",
                "<p>Cuerpo HTML</p>",
                "Cuerpo texto",
                List.of());

        Message message = JavaMailHelper.getMessage(mail, SESSION);

        Address[] cc = message.getRecipients(Message.RecipientType.CC);
        assertEquals(1, cc.length);
        assertEquals("copia@x.com", cc[0].toString());
    }

    @Test
    void getMessage_conBccNoVacio_estableceCabeceraBcc() throws Exception {
        Mail mail = new Mail(
                List.of("destinatario@x.com"),
                List.of(),
                List.of("oculto@x.com"),
                "remitente@x.com",
                "Asunto de prueba",
                "<p>Cuerpo HTML</p>",
                "Cuerpo texto",
                List.of());

        Message message = JavaMailHelper.getMessage(mail, SESSION);

        Address[] bcc = message.getRecipients(Message.RecipientType.BCC);
        assertEquals(1, bcc.length);
        assertEquals("oculto@x.com", bcc[0].toString());
    }

    @Test
    void getMessage_conCcYBccVacios_noEstableceEsasCabeceras() throws Exception {
        Mail mail = new Mail(
                List.of("destinatario@x.com"),
                List.of(),
                List.of(),
                "remitente@x.com",
                "Asunto de prueba",
                "<p>Cuerpo HTML</p>",
                "Cuerpo texto",
                List.of());

        Message message = JavaMailHelper.getMessage(mail, SESSION);

        assertNull(message.getRecipients(Message.RecipientType.CC));
        assertNull(message.getRecipients(Message.RecipientType.BCC));
        assertEquals("destinatario@x.com", message.getRecipients(Message.RecipientType.TO)[0].toString());
    }

    @Test
    void getMessage_conCcYBccNulos_noEstableceEsasCabeceras() throws Exception {
        Mail mail = new Mail(
                List.of("destinatario@x.com"),
                null,
                null,
                "remitente@x.com",
                "Asunto de prueba",
                "<p>Cuerpo HTML</p>",
                "Cuerpo texto",
                List.of());

        Message message = JavaMailHelper.getMessage(mail, SESSION);

        assertNull(message.getRecipients(Message.RecipientType.CC));
        assertNull(message.getRecipients(Message.RecipientType.BCC));
        assertEquals("destinatario@x.com", message.getRecipients(Message.RecipientType.TO)[0].toString());
    }
}
