package com.educaflow.subsystem.correos.service.impl;

import com.axelor.app.AppSettings;
import com.educaflow.subsystem.correos.service.RemitenteProvider;

/**
 * Lee el remitente desde {@code mail.smtp.user} (configuración privada). Es el único punto que
 * obtiene este valor de {@link AppSettings}, de modo que el {@code from} no puede divergir de la
 * cuenta SMTP autenticada por {@code MailSenderProvider}.
 */
public class RemitenteProviderImpl implements RemitenteProvider {

    private static final String CONFIG_SMTP_USER = "mail.smtp.user";

    @Override
    public String getFrom() {
        String from = AppSettings.get().get(CONFIG_SMTP_USER);
        if (from == null || from.isBlank()) {
            throw new IllegalStateException(
                    "Falta configurar la propiedad '" + CONFIG_SMTP_USER
                            + "' (remitente de los correos).");
        }
        return from;
    }
}
