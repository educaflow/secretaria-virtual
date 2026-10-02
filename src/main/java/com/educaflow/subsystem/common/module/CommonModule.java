package com.educaflow.subsystem.common.module;

import com.axelor.app.AxelorModule;
import com.educaflow.subsystem.common.service.DirectorCentroService;
import com.educaflow.subsystem.common.service.impl.DirectorCentroServiceImpl;

public class CommonModule extends AxelorModule {

    @Override
    protected void configure() {
        bind(DirectorCentroService.class).to(DirectorCentroServiceImpl.class);
    }
}
