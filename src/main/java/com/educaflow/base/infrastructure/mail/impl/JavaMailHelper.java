package com.educaflow.base.infrastructure.mail.impl;



import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import com.educaflow.base.infrastructure.mail.Mail;
import com.educaflow.base.infrastructure.mail.Attach;
import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;

/**
 * Clase que crear el mensaje de EMail a partir de la clase Mail
 * Se usa para no repetir texto entre las distintas implementaciones del MailService
 * @author logongas
 */
public class JavaMailHelper {

    public static Message getMessage(Mail mail, Session session) {
        try {
            MimeMessage message = new MimeMessage(session);

            message.setSubject(mail.subject(), "UTF-8");
            message.setFrom(new InternetAddress(mail.from()));
            message.setRecipients(jakarta.mail.Message.RecipientType.TO, getAddresses(mail.to()));
            if (mail.cc() != null && !mail.cc().isEmpty()) {
                message.setRecipients(jakarta.mail.Message.RecipientType.CC, getAddresses(mail.cc()));
            }
            if (mail.bcc() != null && !mail.bcc().isEmpty()) {
                message.setRecipients(jakarta.mail.Message.RecipientType.BCC, getAddresses(mail.bcc()));
            }

            MimeMultipart mimeMultiPart = new MimeMultipart("alternative");

            if (mail.textBody() != null && !mail.textBody().isEmpty()) {
                MimeBodyPart textPart = new MimeBodyPart();
                textPart.setContent(mail.textBody(), "text/plain; charset=UTF-8");
                mimeMultiPart.addBodyPart(textPart);
            }

            if (mail.htmlBody() != null && !mail.htmlBody().isEmpty()) {
                MimeBodyPart htmlPart = new MimeBodyPart();
                htmlPart.setContent(mail.htmlBody(), "text/html; charset=UTF-8");
                mimeMultiPart.addBodyPart(htmlPart);
            }

            MimeBodyPart contentBodyPart = new MimeBodyPart();
            contentBodyPart.setContent(mimeMultiPart);


            MimeMultipart mainBodyPart = new MimeMultipart("mixed");
            mainBodyPart.addBodyPart(contentBodyPart);
            message.setContent(mainBodyPart);


            for (Attach attach : mail.attachs()) {
                MimeBodyPart att = new MimeBodyPart();
                DataSource dataSource = new InputStreamDataSource(attach.mimeType(), attach.fileName(), attach.data());
                att.setDataHandler(new DataHandler(dataSource));
                att.setFileName(dataSource.getName());
                mainBodyPart.addBodyPart(att);
            }


            return message;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

    }

    private static Address[] getAddresses(List<String> addresses) {
        try {
            List<Address> addressesList = new ArrayList<Address>();

            for (String address : addresses) {
                addressesList.add(new InternetAddress(address));
            }

            Address[] addressesArray = new Address[addressesList.size()];
            addressesList.toArray(addressesArray);
            return addressesArray;
        } catch (AddressException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static class InputStreamDataSource implements DataSource {

        private final String contentType;
        private final String name;
        private final byte[] data;

        public InputStreamDataSource(String contentType, String name, byte[] data) {
            this.contentType = contentType;
            this.name = name;
            this.data = data;
        }

        public String getContentType() {
            return contentType;
        }

        public String getName() {
            return name;
        }

        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(data);
        }

        public OutputStream getOutputStream() throws IOException {
            throw new UnsupportedOperationException("Not implemented");
        }
    }
}
