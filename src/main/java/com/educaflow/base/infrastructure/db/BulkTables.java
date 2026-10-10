package com.educaflow.base.infrastructure.db;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

public class BulkTables {

    private static final Logger logger = LoggerFactory.getLogger(BulkTables.class);

    public void truncateTables(String dataBaseDriver, String dataBaseURL, String dataBaseUser, String dataBasePassword, String schemaName, List<String> prefijosTruncados, Set<String> tablasExcluidas, Set<String> tablasIncluidas)  {

        try {
            logger.info("Conectando a la base de datos...");
            Class.forName(dataBaseDriver);
            try (Connection connection = DriverManager.getConnection(dataBaseURL, dataBaseUser, dataBasePassword)) {
                connection.setAutoCommit(false);

                logger.info("Desactivando restricciones...");
                disableAllTriggers(connection, schemaName);

                for (String prefijo : prefijosTruncados) {
                    truncateTablesByLike(connection, schemaName, prefijo.replace("_", "\\_") + "%", tablasExcluidas);
                }
                truncateTablesByName(connection, schemaName, tablasIncluidas);


                enableAllTriggers(connection, schemaName);

                connection.commit();
                logger.info("Operación completada exitosamente.");
            } catch (Exception ex) {
                throw new RuntimeException("Fallo al conectar a la base de datos:dataBaseURL="+dataBaseURL+",dataBaseUser="+dataBaseUser+",dataBasePassword="+dataBasePassword, ex);
            }
        } catch (Exception ex) {
            throw new RuntimeException("Fallo al borrar los meta datos", ex);
        }
    }

    private void disableAllTriggers(Connection connection, String schemaName) throws SQLException {
        DatabaseSchema databaseSchema = new DatabaseSchema(connection, schemaName);
        List<Table> tables = databaseSchema.getAllTables();

        for (Table table : tables) {
            table.disableAllTriggers();
        }
    }


    private void enableAllTriggers(Connection connection, String schemaName) throws SQLException {
        DatabaseSchema databaseSchema = new DatabaseSchema(connection, schemaName);
        List<Table> tables = databaseSchema.getAllTables();

        for (Table table : tables) {
            table.enableAllTriggers();
        }
    }


    private void truncateTablesByLike(Connection connection, String schemaName, String like, Set<String> tablasExcluidas) {
        logger.info("Borrando contenido de las tablas que empiezan por '{}'", like);
        DatabaseSchema databaseSchema = new DatabaseSchema(connection, schemaName);
        List<Table> tables = databaseSchema.getTablesByLike(like);

        for (Table table : tables) {
            String tableName = table.getTableName();
            if (!tablasExcluidas.contains(tableName)) {
                logger.info("Borrando contenido de la tabla {}", tableName);
                table.truncate();
            }
        }

    }

    private void truncateTablesByName(Connection connection, String schemaName, Set<String> tablasIncluidas) {
        DatabaseSchema databaseSchema = new DatabaseSchema(connection, schemaName);

        for (String tableName : tablasIncluidas) {
            databaseSchema.getTable(tableName).ifPresentOrElse(table -> {
                logger.info("Borrando contenido de la tabla {}", tableName);
                table.truncate();
            }, () -> logger.warn("La tabla '{}' no existe en el esquema '{}'", tableName, schemaName));
        }
    }

}


