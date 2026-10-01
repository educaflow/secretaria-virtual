package com.educaflow.secretariavirtual.module;

import com.axelor.app.AxelorModule;
import com.educaflow.base.infrastructure.async.EjecutorAsincrono;
import com.educaflow.secretariavirtual.startup.AppEventObserver;
import com.educaflow.secretariavirtual.startup.DataBaseStartup;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SecretariaVirtualModule extends AxelorModule {

    private final Logger logger = LoggerFactory.getLogger(SecretariaVirtualModule.class);

    @Override
    protected void configure() {
        logger.info("Iniciando Módulo de la Secretaria Virtual...");

        bind(AppEventObserver.class);
        bind(EjecutorAsincrono.class).toProvider(EjecutorAsincronoProvider.class).in(Singleton.class);

        DataBaseStartup.truncateTables();
    }


}
