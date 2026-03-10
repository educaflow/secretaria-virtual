package com.educaflow.base.infrastructure.mail.impl;

import com.educaflow.base.infrastructure.mail.Mail;
import com.educaflow.base.infrastructure.mail.MailSender;

import java.util.Properties;

import jakarta.annotation.Nonnull;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;

public class MailSenderImpl implements MailSender {
    final SmtpCredentialSimplePassword smtpCredentialImplSimplePassword;

    public MailSenderImpl(@Nonnull SmtpCredentialSimplePassword smtpCredentialImplSimplePassword) {
        if (smtpCredentialImplSimplePassword==null) {
            throw new IllegalArgumentException("smtpCredentialImplSimplePassword no puede ser null");
        }

        this.smtpCredentialImplSimplePassword=smtpCredentialImplSimplePassword;
    }


    @Override
    public void send(Mail mail) {
        try {
            if (mail==null) {
                throw new IllegalArgumentException("Mail cannot be null");
            }

            String smtpHost = smtpCredentialImplSimplePassword.host();
            String smtpUserName = smtpCredentialImplSimplePassword.userName();
            String smtpPassword = smtpCredentialImplSimplePassword.password();

            Properties properties = new Properties();
            properties.put("mail.transport.protocol", "smtp");
            properties.put("mail.smtp.host", smtpHost);
            properties.put("mail.smtp.port", "587");
            properties.put("mail.smtp.auth", "true");
            properties.put("mail.smtp.starttls.enable", "true");
            properties.put("mail.smtp.starttls.required", "true");
            properties.put("mail.smtp.auth.mechanisms", "LOGIN PLAIN");

            Authenticator authenticator = new SMTPAuthenticator(smtpUserName, smtpPassword);

            Session session = Session.getInstance(properties, authenticator);

            Message message=JavaMailHelper.getMessage(mail, session);

/*
            Transport transport = session.getTransport();
            transport.connect();
            transport.sendMessage(message,message.getRecipients(jakarta.mail.Message.RecipientType.TO));
            transport.close();
*/

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
