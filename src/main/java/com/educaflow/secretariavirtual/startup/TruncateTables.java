package com.educaflow.secretariavirtual.startup;

import com.educaflow.base.infrastructure.db.BulkTables;

import java.util.Set;

public class TruncateTables {

    public void truncateTables(String dataBaseDriver, String dataBaseURL, String dataBaseUser, String dataBasePassword, String schemaName)  {
        Set<String> tablasExcluidas = Set.of("meta_file", "meta_sequence", "auth_user", "auth_group", "meta_filter");
        Set<String> tablasIncluidas = Set.of("expedientes_estado_tipo_expediente");

        BulkTables bulkTables = new BulkTables();
        bulkTables.truncateTables(dataBaseDriver,dataBaseURL,dataBaseUser,dataBasePassword,schemaName,tablasExcluidas,tablasIncluidas);

    }


}
