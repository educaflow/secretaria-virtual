package com.educaflow.base.infrastructure.mail;

import com.educaflow.base.infrastructure.mail.impl.GmailApiMailSender;
import com.educaflow.base.infrastructure.mail.impl.MailSenderImplSmtp;

import java.util.Objects;

public class MailSenderFactory {

    public static MailSender getSmtpMailSender(UserPasswordCredential userPasswordCredential) {
        Objects.requireNonNull(userPasswordCredential, "userPasswordCredential no puede ser null");
        return new MailSenderImplSmtp(userPasswordCredential);
    }

    public static MailSender getGMailApiMailSender(GMailApiCredential gMailApiCredential) {
        Objects.requireNonNull(gMailApiCredential, "gMailApiCredential no puede ser null");
        return new GmailApiMailSender(gMailApiCredential);
    }

}