package com.educaflow.base.infrastructure.mail;

import com.educaflow.base.infrastructure.mail.impl.GmailApiMailSender;
import com.educaflow.base.infrastructure.mail.impl.MailSenderImplSmtp;

public class MailSenderFactory {

    public static MailSender getSmtpMailSender(UserPasswordCredential userPasswordCredential) {
        if (userPasswordCredential == null) {
            throw new IllegalArgumentException("userPasswordCredential no puede ser null");
        }
        return new MailSenderImplSmtp(userPasswordCredential);
    }

    public static MailSender getGMailApiMailSender(GMailApiCredential gMailApiCredential) {
        if (gMailApiCredential == null) {
            throw new IllegalArgumentException("gMailApiCredential no puede ser null");
        }
        return new GmailApiMailSender(gMailApiCredential.clientId(), gMailApiCredential.projectId(), gMailApiCredential.clientSecret(), gMailApiCredential.refreshToken());
    }

}