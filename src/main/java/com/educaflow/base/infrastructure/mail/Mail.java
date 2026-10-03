package com.educaflow.base.infrastructure.mail;

import com.educaflow.base.infrastructure.fichero.Fichero;
import com.educaflow.base.util.TextUtil;
import java.util.Objects;

public record Mail(java.util.List<String> to, java.util.List<String> cc, java.util.List<String> bcc,
                   String from, String subject, String htmlBody, String textBody,
                   java.util.List<Fichero> attachs) {

    public Mail {
        Objects.requireNonNull(to, "to no puede ser null");
        TextUtil.requireNonBlank(from, "from no puede ser null ni blank");
        TextUtil.requireNonBlank(subject, "subject no puede ser null ni blank");
        Objects.requireNonNull(attachs, "attachs no puede ser null");
    }

    public Mail(java.util.List<String> to, String from, String subject, String htmlBody,
                String textBody, java.util.List<Fichero> attachs) {
        this(to, java.util.List.of(), java.util.List.of(), from, subject, htmlBody, textBody, attachs);
    }
}
