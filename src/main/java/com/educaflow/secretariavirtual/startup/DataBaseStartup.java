package com.educaflow.secretariavirtual.startup;

import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import com.educaflow.base.infrastructure.db.BulkTables;
import org.flywaydb.core.Flyway;

import java.util.Set;

public class DataBaseStartup {

    public static void startup()  {
        executeMigrate(dbDriver(), dbURL(), dbUser(), dbPassword(), "public");
    }

    public static void truncateTables() {
        truncateTables(dbDriver(), dbURL(), dbUser(), dbPassword(), "public");
    }

    private static String dbDriver() { return AppSettings.get().get(AvailableAppSettings.DB_DEFAULT_DRIVER); }
    private static String dbURL() { return AppSettings.get().get(AvailableAppSettings.DB_DEFAULT_URL); }
    private static String dbUser() { return AppSettings.get().get(AvailableAppSettings.DB_DEFAULT_USER); }
    private static String dbPassword() { return AppSettings.get().get(AvailableAppSettings.DB_DEFAULT_PASSWORD); }

    private static void truncateTables(String dataBaseDriver, String dataBaseURL, String dataBaseUser, String dataBasePassword, String schemaName) {
        Set<String> tablasExcluidas = Set.of("meta_file", "meta_sequence", "auth_user", "auth_group", "meta_filter");
        Set<String> tablasIncluidas = Set.of("expedientes_estado_tipo_expediente");

        BulkTables bulkTables = new BulkTables();
        bulkTables.truncateTables(dataBaseDriver,dataBaseURL,dataBaseUser,dataBasePassword,schemaName,tablasExcluidas,tablasIncluidas);
    }


    private static void executeMigrate(String dataBaseDriver, String dataBaseURL, String dataBaseUser, String dataBasePassword, String schemaName) {
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
