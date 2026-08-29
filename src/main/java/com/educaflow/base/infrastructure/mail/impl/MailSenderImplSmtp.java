package com.educaflow.base.infrastructure.mail.impl;

import com.educaflow.base.infrastructure.mail.Mail;
import com.educaflow.base.infrastructure.mail.MailSender;

import java.util.Properties;

import com.educaflow.base.infrastructure.mail.UserPasswordCredential;
import jakarta.annotation.Nonnull;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;

public class MailSenderImplSmtp implements MailSender {
    final UserPasswordCredential userPasswordCredential;

    public MailSenderImplSmtp(@Nonnull UserPasswordCredential userPasswordCredential) {
        if (userPasswordCredential ==null) {
            throw new IllegalArgumentException("smtpCredentialImplSimplePassword no puede ser null");
        }

        this.userPasswordCredential = userPasswordCredential;
    }


    @Override
    public void send(Mail mail) {
        try {
            if (mail==null) {
                throw new IllegalArgumentException("Mail cannot be null");
            }

            String smtpHost = userPasswordCredential.host();
            String smtpUserName = userPasswordCredential.userName();
            String smtpPassword = userPasswordCredential.password();

            Properties properties = new Properties();
            properties.put("mail.transport.protocol", "smtp");
            properties.put("mail.smtp.host", smtpHost);
            properties.put("mail.smtp.port", "587");
            properties.put("mail.smtp.auth", "true");
            properties.put("mail.smtp.starttls.enable", "true");
            properties.put("mail.smtp.starttls.required", "true");
            properties.put("mail.smtp.auth.mechanisms", "LOGIN PLAIN");

            // Timeouts (10s) para que no se cuelgue si el servidor no responde
            properties.put("mail.smtp.connectiontimeout", "10000");
            properties.put("mail.smtp.timeout", "10000");
            properties.put("mail.smtp.writetimeout", "10000");

            Authenticator authenticator = new SMTPAuthenticator(smtpUserName, smtpPassword);

            Session session = Session.getInstance(properties, authenticator);

            Message message=JavaMailHelper.getMessage(mail, session);

            try (Transport transport = session.getTransport("smtp")) {
                transport.connect();
                transport.sendMessage(message, message.getAllRecipients());
            }

        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }


    private class SMTPAuthenticator extends jakarta.mail.Authenticator {
        private final String userName;
        private final String password;

        public SMTPAuthenticator(String userName, String password) {
            this.userName = userName;
            this.password = password;
        }

        @Override
        public PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(userName, password);
        }
    }

}
