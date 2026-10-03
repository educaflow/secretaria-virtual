package com.educaflow.base.infrastructure.mail.impl;

import com.educaflow.base.infrastructure.mail.GMailApiCredential;
import com.educaflow.base.infrastructure.mail.Mail;
import com.educaflow.base.infrastructure.mail.MailSender;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.UserCredentials;

import jakarta.mail.Session;


import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Objects;
import java.util.Properties;

public class GmailApiMailSender implements MailSender {

    private static final String APPLICATION_NAME = "My First Project";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    private final Gmail gmailService;

    public GmailApiMailSender(GMailApiCredential gMailApiCredential) {
        Objects.requireNonNull(gMailApiCredential, "gMailApiCredential no puede ser null");
        try {
            UserCredentials credentials = UserCredentials.newBuilder()
                    .setClientId(gMailApiCredential.clientId())
                    .setClientSecret(gMailApiCredential.clientSecret())
                    .setRefreshToken(gMailApiCredential.refreshToken())
                    .build();

            this.gmailService = new Gmail.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    JSON_FACTORY,
                    new HttpCredentialsAdapter(credentials))
                    .setApplicationName(APPLICATION_NAME)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Error inicializando el cliente de Gmail API", e);
        }
    }


    @Override
    public void send(Mail mail) {
        Objects.requireNonNull(mail, "mail no puede ser null");
        try {
            jakarta.mail.Message mimeMessage = JavaMailHelper.getMessage(mail, Session.getInstance(new Properties()));
            Message message = createMessageWithEmail(mimeMessage);

            // "me" indica que el correo se envía desde el usuario autenticado
            gmailService.users().messages().send("me", message).execute();

        } catch (Exception e) {
            throw new RuntimeException("Fallo al enviar correo mediante Gmail API", e);
        }
    }

    private Message createMessageWithEmail(jakarta.mail.Message emailContent) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        emailContent.writeTo(buffer);
        byte[] bytes = buffer.toByteArray();

        // Gmail API requiere base64url encoding seguro
        String encodedEmail = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        Message message = new Message();
        message.setRaw(encodedEmail);
        return message;
    }


}
