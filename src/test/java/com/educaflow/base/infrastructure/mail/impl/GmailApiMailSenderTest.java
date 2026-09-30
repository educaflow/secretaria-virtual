package com.educaflow.base.infrastructure.mail.impl;

import com.educaflow.base.infrastructure.mail.Attach;
import com.educaflow.base.infrastructure.mail.Mail;
import jakarta.mail.Address;
import jakarta.mail.BodyPart;
import jakarta.mail.Message;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GmailApiMailSenderTest {

    private static MimeMessage createMimeMessage(Mail mail) throws Exception {
        GmailApiMailSender sender = new GmailApiMailSender("clientId", "projectId", "clientSecret", "refreshToken");
        Method method = GmailApiMailSender.class.getDeclaredMethod("createMimeMessage", Mail.class);
        method.setAccessible(true);
        try {
            MimeMessage message = (MimeMessage) method.invoke(sender, mail);
            message.saveChanges();
            return message;
        } catch (InvocationTargetException e) {
            throw (Exception) e.getCause();
        }
    }

    private static MimeMultipart bodyOf(MimeMessage message) throws Exception {
        MimeMultipart mixed = (MimeMultipart) message.getContent();
        return (MimeMultipart) mixed.getBodyPart(0).getContent();
    }

    @Test
    void createMimeMessage_conTodo_rellenaCabecerasCuerposYAdjuntos() throws Exception {
        byte[] datos = "contenido".getBytes(StandardCharsets.UTF_8);
        Mail mail = new Mail(
                List.of("a@x.com", "b@x.com"),
                List.of("copia@x.com"),
                List.of("oculto@x.com"),
                "remitente@x.com",
                "Asunto",
                "<p>html</p>",
                "texto",
                List.of(new Attach("fichero.pdf", datos, "application/pdf")));

        MimeMessage message = createMimeMessage(mail);

        assertEquals("remitente@x.com", message.getFrom()[0].toString());
        Address[] to = message.getRecipients(Message.RecipientType.TO);
        assertEquals(2, to.length);
        assertEquals("a@x.com", to[0].toString());
        assertEquals("b@x.com", to[1].toString());
        assertEquals("copia@x.com", message.getRecipients(Message.RecipientType.CC)[0].toString());
        assertEquals("oculto@x.com", message.getRecipients(Message.RecipientType.BCC)[0].toString());
        assertEquals("Asunto", message.getSubject());

        MimeMultipart mixed = (MimeMultipart) message.getContent();
        assertTrue(mixed.getContentType().startsWith("multipart/mixed"));
        assertEquals(2, mixed.getCount());

        MimeMultipart body = bodyOf(message);
        assertTrue(body.getContentType().startsWith("multipart/alternative"));
        assertEquals(2, body.getCount());
        assertTrue(body.getBodyPart(0).isMimeType("text/plain"));
        assertEquals("texto", body.getBodyPart(0).getContent());
        assertTrue(body.getBodyPart(1).isMimeType("text/html"));
        assertEquals("<p>html</p>", body.getBodyPart(1).getContent());

        BodyPart adjunto = mixed.getBodyPart(1);
        assertEquals("fichero.pdf", adjunto.getFileName());
        assertTrue(adjunto.isMimeType("application/pdf"));
        try (InputStream in = adjunto.getInputStream()) {
            assertArrayEquals(datos, in.readAllBytes());
        }
    }

    @Test
    void createMimeMessage_conCuerposYListasNulos_soloCreaElContenedorDelCuerpoVacio() throws Exception {
        Mail mail = new Mail(null, null, null, "remitente@x.com", "Asunto", null, null, null);

        MimeMessage message = createMimeMessage(mail);

        assertNull(message.getRecipients(Message.RecipientType.TO));
        assertNull(message.getRecipients(Message.RecipientType.CC));
        assertNull(message.getRecipients(Message.RecipientType.BCC));
        MimeMultipart mixed = (MimeMultipart) message.getContent();
        assertEquals(1, mixed.getCount());
        assertEquals(0, bodyOf(message).getCount());
    }

    @Test
    void createMimeMessage_conCuerposYListasVacios_soloCreaElContenedorDelCuerpoVacio() throws Exception {
        Mail mail = new Mail(List.of("a@x.com"), List.of(), List.of(), "remitente@x.com", "Asunto", "", "", List.of());

        MimeMessage message = createMimeMessage(mail);

        assertEquals(1, message.getRecipients(Message.RecipientType.TO).length);
        assertNull(message.getRecipients(Message.RecipientType.CC));
        assertNull(message.getRecipients(Message.RecipientType.BCC));
        MimeMultipart mixed = (MimeMultipart) message.getContent();
        assertEquals(1, mixed.getCount());
        assertEquals(0, bodyOf(message).getCount());
    }

    @Test
    void createMimeMessage_soloConHtml_anhadeSoloLaParteHtml() throws Exception {
        Mail mail = new Mail(List.of("a@x.com"), "remitente@x.com", "Asunto", "<b>hola</b>", null, List.of());

        MimeMultipart body = bodyOf(createMimeMessage(mail));

        assertEquals(1, body.getCount());
        assertTrue(body.getBodyPart(0).isMimeType("text/html"));
        assertEquals("<b>hola</b>", body.getBodyPart(0).getContent());
    }

    @Test
    void createMimeMessage_soloConTexto_anhadeSoloLaParteTexto() throws Exception {
        Mail mail = new Mail(List.of("a@x.com"), "remitente@x.com", "Asunto", null, "hola", List.of());

        MimeMultipart body = bodyOf(createMimeMessage(mail));

        assertEquals(1, body.getCount());
        assertTrue(body.getBodyPart(0).isMimeType("text/plain"));
        assertEquals("hola", body.getBodyPart(0).getContent());
    }
}
