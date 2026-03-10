package com.educaflow.shared.registroentradasalida.module;

import com.axelor.app.AxelorModule;

import com.educaflow.shared.registroentradasalida.service.RegistroEntradaService;
import com.educaflow.shared.registroentradasalida.service.RegistroSalidaService;
import com.educaflow.shared.registroentradasalida.service.impl.RegistroEntradaServiceImpl;
import com.educaflow.shared.registroentradasalida.service.impl.RegistroSalidaServiceImpl;

public class RegistroEntradaSalidaModule  extends AxelorModule {

    @Override
    protected void configure() {
        bind(RegistroEntradaService.class).to(RegistroEntradaServiceImpl.class);
        bind(RegistroSalidaService.class).to(RegistroSalidaServiceImpl.class);
    }
}
