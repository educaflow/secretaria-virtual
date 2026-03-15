package com.educaflow.subsystem.registroentradasalida.module;

import com.axelor.app.AxelorModule;

import com.educaflow.subsystem.registroentradasalida.service.RegistroEntradaService;
import com.educaflow.subsystem.registroentradasalida.service.RegistroSalidaService;
import com.educaflow.subsystem.registroentradasalida.service.impl.RegistroEntradaServiceImpl;
import com.educaflow.subsystem.registroentradasalida.service.impl.RegistroSalidaServiceImpl;

public class RegistroEntradaSalidaModule  extends AxelorModule {

    @Override
    protected void configure() {
        bind(RegistroEntradaService.class).to(RegistroEntradaServiceImpl.class);
        bind(RegistroSalidaService.class).to(RegistroSalidaServiceImpl.class);
    }
}
