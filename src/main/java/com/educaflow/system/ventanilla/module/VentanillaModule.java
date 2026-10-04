package com.educaflow.system.ventanilla.module;

import com.axelor.app.AxelorModule;
import com.educaflow.system.ventanilla.service.BandejaService;
import com.educaflow.system.ventanilla.service.impl.BandejaServiceImpl;

public class VentanillaModule extends AxelorModule {

    @Override
    protected void configure() {
        bind(BandejaService.class).to(BandejaServiceImpl.class);
    }
}
