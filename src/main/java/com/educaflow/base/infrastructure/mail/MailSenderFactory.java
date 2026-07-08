package com.educaflow.base.infrastructure.mail;

import com.educaflow.base.infrastructure.mail.impl.MailSenderImplSmtp;
import com.educaflow.base.infrastructure.mail.impl.SmtpCredentialSimplePassword;

public class MailSenderFactory {

    public static MailSender getSmtpMailSender(SmtpCredentialSimplePassword smtpCredentialSimplePassword) {
        return new MailSenderImplSmtp(smtpCredentialSimplePassword);
    }

}