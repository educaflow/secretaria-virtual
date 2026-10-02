package com.educaflow.base.infrastructure.mail;

import com.educaflow.base.infrastructure.fichero.Fichero;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MailTest {

    @Test
    void constructorCompatibilidad_seisArgumentos_delegaConCcYBccVacios() {
        List<String> to = List.of("destinatario@example.com");
        String from = "remitente@example.com";
        String subject = "Asunto de prueba";
        String htmlBody = "<p>Cuerpo HTML</p>";
        String textBody = "Cuerpo texto";
        List<Fichero> attachs = List.of(new Fichero("fichero.pdf", new byte[]{1, 2, 3}, "application/pdf"));

        Mail mail = new Mail(to, from, subject, htmlBody, textBody, attachs);

        assertTrue(mail.cc().isEmpty());
        assertTrue(mail.bcc().isEmpty());
        assertEquals(to, mail.to());
        assertEquals(from, mail.from());
        assertEquals(subject, mail.subject());
        assertEquals(htmlBody, mail.htmlBody());
        assertEquals(textBody, mail.textBody());
        assertEquals(attachs, mail.attachs());
    }
}
