package com.educaflow.subsystem.firmas.module;

import com.axelor.app.AxelorModule;
import com.educaflow.subsystem.firmas.service.FirmaService;
import com.educaflow.subsystem.firmas.service.impl.FirmaServiceImpl;

public class FirmaModule extends AxelorModule {

    @Override
    protected void configure() {
        bind(FirmaService.class).to(FirmaServiceImpl.class);
    }
}
