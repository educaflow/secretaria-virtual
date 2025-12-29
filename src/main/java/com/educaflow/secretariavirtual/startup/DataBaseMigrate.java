package com.educaflow.secretariavirtual.startup;

import org.flywaydb.core.Flyway;


public class DataBaseMigrate {

    public void migrate(String dataBaseDriver, String dataBaseURL, String dataBaseUser, String dataBasePassword, String schemaName)   {
        Flyway flyway = Flyway.configure()
                .dataSource(dataBaseURL, dataBaseUser, dataBasePassword)
                .driver(dataBaseDriver)
                .schemas(schemaName)
                .locations("classpath:com/educaflow/secretariavirtual/startup/database")
                .baselineOnMigrate(true)
                .load();

        flyway.migrate();
    }

}
