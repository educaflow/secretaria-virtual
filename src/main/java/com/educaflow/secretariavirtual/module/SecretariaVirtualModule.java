package com.educaflow.secretariavirtual.module;

import com.axelor.app.AxelorModule;
import com.educaflow.secretariavirtual.startup.AppEventObserver;
import com.educaflow.secretariavirtual.startup.DataBaseStartup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SecretariaVirtualModule extends AxelorModule {

    private final Logger logger = LoggerFactory.getLogger(SecretariaVirtualModule.class);

    @Override
    protected void configure() {
        logger.info("Iniciando Módulo de la Secretaria Virtual...");

        bind(AppEventObserver.class);

        DataBaseStartup.truncateTables();
    }


}
