package com.educaflow.secretariavirtual.startup;

import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import com.axelor.event.Observes;
import com.axelor.events.StartupEvent;
import org.flywaydb.core.Flyway;

public class DataBaseMigrate {


    public void onAppStartup(@Observes StartupEvent event) {
        String dataBaseDriver = AppSettings.get().get(AvailableAppSettings.DB_DEFAULT_DRIVER);
        String dataBaseURL = AppSettings.get().get(AvailableAppSettings.DB_DEFAULT_URL);
        String dataBaseUser = AppSettings.get().get(AvailableAppSettings.DB_DEFAULT_USER);
        String dataBasePassword = AppSettings.get().get(AvailableAppSettings.DB_DEFAULT_PASSWORD);
        String schemaName = "public";
        executeMigrate(dataBaseDriver, dataBaseURL, dataBaseUser, dataBasePassword, schemaName);
    }


    private void executeMigrate(String dataBaseDriver, String dataBaseURL, String dataBaseUser, String dataBasePassword, String schemaName) {
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
