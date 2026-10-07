package com.educaflow.subsystem.criptografia.module;

import com.axelor.app.AxelorModule;
import com.educaflow.subsystem.criptografia.service.FirmaEnServidorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CriptografiaModule extends AxelorModule {

    private final Logger logger = LoggerFactory.getLogger(CriptografiaModule.class);

    @Override
    protected void configure() {
        bind(FirmaEnServidorService.class);
    }

}
