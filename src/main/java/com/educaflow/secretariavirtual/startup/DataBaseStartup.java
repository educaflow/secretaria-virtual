package com.educaflow.secretariavirtual.startup;

import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import com.educaflow.base.infrastructure.db.BulkTables;
import org.flywaydb.core.Flyway;

import java.util.List;
import java.util.Set;

public class DataBaseStartup {

    /**
     * Política de truncado al arrancar: se vacían las tablas que empiezan por uno de {@link #PREFIJOS_TRUNCADOS}
     * salvo las de {@link #TABLAS_EXCLUIDAS}, y además las de {@link #TABLAS_INCLUIDAS}.
     *
     * <p>El truncado es {@code TRUNCATE ... CASCADE}: arrastra a toda tabla con una FK <b>hacia</b> una tabla truncada,
     * esté o no en {@link #TABLAS_EXCLUIDAS}. Que ninguna tabla excluida apunte a una truncada lo comprueba
     * {@code TablasExcluidasDelTruncadoTest}.
     */
    static final List<String> PREFIJOS_TRUNCADOS = List.of("meta_", "auth_");
    static final Set<String> TABLAS_EXCLUIDAS = Set.of("meta_file", "meta_sequence", "auth_user", "auth_group", "meta_filter");
    static final Set<String> TABLAS_INCLUIDAS = Set.of(
            "security_ace_profile_global",
            "security_ace_profile_tipo_usuario_tramite",
            "security_ace_profile_tramite",
            "security_ace_profile_tipo_expediente");

    public static void startup()  {
        executeMigrate(dbDriver(), dbURL(), dbUser(), dbPassword(), "public");
    }

    public static void truncateTables() {
        truncateTables(dbDriver(), dbURL(), dbUser(), dbPassword(), "public");
    }

    /** Si la tabla (en minúsculas) la vacía el arranque, sin contar lo que arrastre el {@code CASCADE}. */
    static boolean seTrunca(String tableName) {
        if (TABLAS_INCLUIDAS.contains(tableName)) {
            return true;
        }
        if (TABLAS_EXCLUIDAS.contains(tableName)) {
            return false;
        }
        return PREFIJOS_TRUNCADOS.stream().anyMatch(tableName::startsWith);
    }

    private static String dbDriver() { return AppSettings.get().get(AvailableAppSettings.DB_DEFAULT_DRIVER); }
    private static String dbURL() { return AppSettings.get().get(AvailableAppSettings.DB_DEFAULT_URL); }
    private static String dbUser() { return AppSettings.get().get(AvailableAppSettings.DB_DEFAULT_USER); }
    private static String dbPassword() { return AppSettings.get().get(AvailableAppSettings.DB_DEFAULT_PASSWORD); }

    private static void truncateTables(String dataBaseDriver, String dataBaseURL, String dataBaseUser, String dataBasePassword, String schemaName) {
        BulkTables bulkTables = new BulkTables();
        bulkTables.truncateTables(dataBaseDriver, dataBaseURL, dataBaseUser, dataBasePassword, schemaName, PREFIJOS_TRUNCADOS, TABLAS_EXCLUIDAS, TABLAS_INCLUIDAS);
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
