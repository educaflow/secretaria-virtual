package com.educaflow.secretariavirtual.startup;

import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import com.educaflow.base.infrastructure.db.BulkTables;

import java.util.Set;

public class TruncateTables {

    public void truncateTables()  {
        String dataBaseDriver = AppSettings.get().get(AvailableAppSettings.DB_DEFAULT_DRIVER);
        String dataBaseURL = AppSettings.get().get(AvailableAppSettings.DB_DEFAULT_URL);
        String dataBaseUser = AppSettings.get().get(AvailableAppSettings.DB_DEFAULT_USER);
        String dataBasePassword = AppSettings.get().get(AvailableAppSettings.DB_DEFAULT_PASSWORD);
        String schemaName = "public";



        Set<String> tablasExcluidas = Set.of("meta_file", "meta_sequence", "auth_user", "auth_group", "meta_filter");
        Set<String> tablasIncluidas = Set.of("expedientes_estado_tipo_expediente");

        BulkTables bulkTables = new BulkTables();
        bulkTables.truncateTables(dataBaseDriver,dataBaseURL,dataBaseUser,dataBasePassword,schemaName,tablasExcluidas,tablasIncluidas);

    }


}
