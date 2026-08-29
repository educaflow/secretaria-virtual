/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.educaflow.base.infrastructure.mail.impl;

import com.educaflow.base.infrastructure.mail.Attach;
import com.educaflow.base.infrastructure.mail.Mail;
import com.educaflow.base.infrastructure.mail.MailSender;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.Message;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.UserCredentials;

import jakarta.activation.DataHandler;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;


import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public class GmailApiMailSender implements MailSender {

    private static final String APPLICATION_NAME = "My First Project";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    private static final List<String> SCOPES = Collections.singletonList(GmailScopes.GMAIL_SEND);
    
    private final Gmail gmailService;
    private final String clientId;
    private final String projectId; 
    private final String clientSecret;
    private final String refreshToken;

    public GmailApiMailSender(String clientId, String projectId, String clientSecret,String refreshToken) {
        try {
            this.clientId=clientId;
            this.projectId=projectId;
            this.clientSecret=clientSecret;
            this.refreshToken=refreshToken;

            UserCredentials credentials = UserCredentials.newBuilder()
                    .setClientId(clientId)
                    .setClientSecret(clientSecret)
                    .setRefreshToken(refreshToken)
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
        try {
            MimeMessage mimeMessage = createMimeMessage(mail);
            Message message = createMessageWithEmail(mimeMessage);
            
            // "me" indica que el correo se envía desde el usuario autenticado
            gmailService.users().messages().send("me", message).execute();
            
        } catch (Exception e) {
            throw new RuntimeException("Fallo al enviar correo mediante Gmail API", e);
        }
    }

    private MimeMessage createMimeMessage(Mail mail) throws MessagingException {
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);
        MimeMessage email = new MimeMessage(session);

        email.setFrom(new InternetAddress(mail.from()));
        
        addRecipients(email, jakarta.mail.Message.RecipientType.TO, mail.to());
        addRecipients(email, jakarta.mail.Message.RecipientType.CC, mail.cc());
        addRecipients(email, jakarta.mail.Message.RecipientType.BCC, mail.bcc());

        email.setSubject(mail.subject());

        // Contenedor principal (mixed si hay adjuntos, alternative si solo hay html/texto)
        MimeMultipart multipart = new MimeMultipart("mixed");

        // Contenedor para el cuerpo del mensaje
        MimeMultipart bodyMultipart = new MimeMultipart("alternative");
        
        if (mail.textBody() != null && !mail.textBody().isEmpty()) {
            MimeBodyPart textPart = new MimeBodyPart();
            textPart.setText(mail.textBody(), "utf-8");
            bodyMultipart.addBodyPart(textPart);
        }
        
        if (mail.htmlBody() != null && !mail.htmlBody().isEmpty()) {
            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent(mail.htmlBody(), "text/html; charset=utf-8");
            bodyMultipart.addBodyPart(htmlPart);
        }

        MimeBodyPart bodyWrapperPart = new MimeBodyPart();
        bodyWrapperPart.setContent(bodyMultipart);
        multipart.addBodyPart(bodyWrapperPart);

        // Adjuntos
        if (mail.attachs() != null && !mail.attachs().isEmpty()) {
            for (Attach attach : mail.attachs()) {
                MimeBodyPart attachmentPart = new MimeBodyPart();
                // Asumiendo métodos getData(), getContentType(), getName() en tu clase Attach
                ByteArrayDataSource bds = new ByteArrayDataSource(attach.data(), attach.mimeType());
                attachmentPart.setDataHandler(new DataHandler(bds));
                attachmentPart.setFileName(attach.fileName());
                multipart.addBodyPart(attachmentPart);
            }
        }

        email.setContent(multipart);
        return email;
    }

    private void addRecipients(MimeMessage email, jakarta.mail.Message.RecipientType type, List<String> recipients) throws MessagingException {
        if (recipients != null && !recipients.isEmpty()) {
            for (String recipient : recipients) {
                email.addRecipient(type, new InternetAddress(recipient));
            }
        }
    }

    private Message createMessageWithEmail(MimeMessage emailContent) throws Exception {
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