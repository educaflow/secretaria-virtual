package com.educaflow.subsystem.sms.module;

import com.axelor.app.AxelorModule;
import com.educaflow.base.infrastructure.sms.SmsSender;

public class SmsModule extends AxelorModule {

    @Override
    protected void configure() {
        bind(SmsSender.class).toProvider(SmsSenderProvider.class);
    }

}
