package com.educaflow.subsystem.correos.module;

import com.axelor.app.AxelorModule;
import com.educaflow.base.infrastructure.mail.MailSender;
import com.educaflow.subsystem.correos.service.RemitenteProvider;
import com.educaflow.subsystem.correos.service.impl.RemitenteProviderImpl;

public class CorreosModule extends AxelorModule {

    @Override
    protected void configure() {
        bind(MailSender.class).toProvider(MailSenderProvider.class);
        bind(RemitenteProvider.class).to(RemitenteProviderImpl.class);
    }
}
