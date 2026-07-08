package com.educaflow.subsystem.correos.module;

import com.axelor.app.AxelorModule;
import com.educaflow.base.infrastructure.mail.MailSender;
import com.educaflow.subsystem.correos.infrastructure.CorreoAsyncExecutor;
import com.educaflow.subsystem.correos.infrastructure.CorreoEventObserver;
import com.google.inject.Singleton;

public class CorreosModule extends AxelorModule {

    @Override
    protected void configure() {
        bind(MailSender.class).toProvider(MailSenderProvider.class);
        bind(CorreoAsyncExecutor.class).toProvider(CorreoAsyncExecutorProvider.class).in(Singleton.class);
        bind(CorreoEventObserver.class);
    }

}
