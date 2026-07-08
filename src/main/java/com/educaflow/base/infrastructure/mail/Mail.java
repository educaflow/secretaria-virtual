package com.educaflow.base.infrastructure.mail;

public record Mail(java.util.List<String> to, java.util.List<String> cc, java.util.List<String> bcc,
                    String from, String subject, String htmlBody, String textBody,
                    java.util.List<Attach> attachs) {

    // Constructor de compatibilidad (firma de 6 argumentos, sin cc/bcc) — delega en el canónico
    // con cc=List.of() y bcc=List.of(). Preserva sin cambios el único llamador real existente
    // (com.educaflow.subsystem.registroentradasalida.service.impl.RegistroSalidaServiceImpl,
    // que sigue construyendo Mail con new Mail(to, from, subject, body, body, attachs)).
    public Mail(java.util.List<String> to, String from, String subject, String htmlBody,
                String textBody, java.util.List<Attach> attachs) {
        this(to, java.util.List.of(), java.util.List.of(), from, subject, htmlBody, textBody, attachs);
    }
}
