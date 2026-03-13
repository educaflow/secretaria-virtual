package com.educaflow.shared.firmas.module;

import com.axelor.app.AxelorModule;
import com.educaflow.shared.firmas.service.FirmaService;
import com.educaflow.shared.firmas.service.impl.FirmaServiceImpl;
import com.educaflow.shared.registroentradasalida.service.RegistroEntradaService;
import com.educaflow.shared.registroentradasalida.service.RegistroSalidaService;
import com.educaflow.shared.registroentradasalida.service.impl.RegistroEntradaServiceImpl;
import com.educaflow.shared.registroentradasalida.service.impl.RegistroSalidaServiceImpl;

public class FirmaModule extends AxelorModule {

    @Override
    protected void configure() {
        bind(FirmaService.class).to(FirmaServiceImpl.class);
    }
}
