package com.educaflow.secretariavirtual.startup;

import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import com.axelor.app.AxelorModule;
import com.educaflow.base.infrastructure.criptografia.EntornoCriptografico;
import com.educaflow.base.infrastructure.criptografia.config.EntornoCriptograficoConfig;

public class Main extends AxelorModule {

    @Override
    protected void configure() {
        System.out.println("Iniciando Aplicación Secretaria Virtual...");

        configureEntornoCriptografico();
        configureDatabase();

    }



    private void configureEntornoCriptografico() {
        EntornoCriptograficoConfigProvider entornoCriptograficoConfigProvider=new EntornoCriptograficoConfigProvider();
        EntornoCriptograficoConfig entornoCriptograficoConfig = entornoCriptograficoConfigProvider.getEntornoCriptograficoConfigFromAppSettings();
        EntornoCriptografico.configure(entornoCriptograficoConfig);
    }

    private void configureDatabase() {
        TruncateTables truncateTables = new TruncateTables();
        truncateTables.truncateTables();

        bind(DataBaseMigrate.class);
    }


}
