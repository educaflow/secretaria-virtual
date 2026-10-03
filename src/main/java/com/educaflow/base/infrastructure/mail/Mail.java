package com.educaflow.base.infrastructure.mail;

import com.educaflow.base.infrastructure.fichero.Fichero;

public record Mail(java.util.List<String> to, java.util.List<String> cc, java.util.List<String> bcc,
                   String from, String subject, String htmlBody, String textBody,
                   java.util.List<Fichero> attachs) {

    // Atajo para el correo sin copias: cc y bcc vacíos.
    public Mail(java.util.List<String> to, String from, String subject, String htmlBody,
                String textBody, java.util.List<Fichero> attachs) {
        this(to, java.util.List.of(), java.util.List.of(), from, subject, htmlBody, textBody, attachs);
    }
}
