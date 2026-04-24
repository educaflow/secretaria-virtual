package com.educaflow.subsystem.common.module;

import com.axelor.app.AxelorModule;
import com.educaflow.subsystem.common.service.CentroService;
import com.educaflow.subsystem.common.service.impl.CentroServiceImpl;

public class CommonModule extends AxelorModule {

    @Override
    protected void configure() {
        //bind(CentroService.class).to(CentroServiceImpl.class);
    }
}
