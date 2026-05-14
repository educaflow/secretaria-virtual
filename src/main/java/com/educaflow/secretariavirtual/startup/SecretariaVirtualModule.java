package com.educaflow.secretariavirtual.startup;

import com.axelor.app.AxelorModule;
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
