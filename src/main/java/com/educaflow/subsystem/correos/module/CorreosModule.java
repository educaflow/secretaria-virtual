package com.educaflow.subsystem.correos.module;

import com.axelor.app.AxelorModule;
import com.educaflow.base.infrastructure.mail.MailSender;

public class CorreosModule extends AxelorModule {

    @Override
    protected void configure() {
        bind(MailSender.class).toProvider(MailSenderProvider.class);
    }

}
