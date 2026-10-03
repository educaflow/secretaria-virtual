package com.educaflow.base.infrastructure.mail.impl;

import com.educaflow.base.infrastructure.mail.GMailApiCredential;
import com.educaflow.base.infrastructure.mail.Mail;
import com.google.api.services.gmail.model.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Base64;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * El MIME lo construye {@link JavaMailHelper} (con sus propios tests): aquí solo se comprueba cómo se
 * empaqueta para la Gmail API.
 */
class GmailApiMailSenderTest {

    private static Message createMessageWithEmail(jakarta.mail.Message mimeMessage) throws Exception {
        GmailApiMailSender sender = new GmailApiMailSender(new GMailApiCredential("clientId", "projectId", "clientSecret", "refreshToken"));
        Method method = GmailApiMailSender.class.getDeclaredMethod("createMessageWithEmail", jakarta.mail.Message.class);
        method.setAccessible(true);
        try {
            return (Message) method.invoke(sender, mimeMessage);
        } catch (InvocationTargetException e) {
            throw (Exception) e.getCause();
        }
    }

    @Test
    void createMessageWithEmail_codificaElMimeEnBase64UrlSinRelleno() throws Exception {
        Session session = Session.getInstance(new Properties());
        Mail mail = new Mail(List.of("a@x.com"), "remitente@x.com", "Asunto", "<b>hola</b>", "hola", List.of());

        Message message = createMessageWithEmail(JavaMailHelper.getMessage(mail, session));

        assertFalse(message.getRaw().contains("="));
        MimeMessage decodificado = new MimeMessage(session, new ByteArrayInputStream(Base64.getUrlDecoder().decode(message.getRaw())));
        assertEquals("Asunto", decodificado.getSubject());
        assertEquals("remitente@x.com", decodificado.getFrom()[0].toString());
        assertEquals("a@x.com", decodificado.getRecipients(jakarta.mail.Message.RecipientType.TO)[0].toString());
    }
}
