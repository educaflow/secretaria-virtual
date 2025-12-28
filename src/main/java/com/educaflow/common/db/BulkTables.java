package com.educaflow.common.db;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

public class BulkTables {

    public void truncateTables(String dataBaseDriver, String dataBaseURL, String dataBaseUser, String dataBasePassword, String schemaName, Set<String> tablasExcluidas, Set<String> tablasIncluidas)  {

        try {
            System.out.println("Conectando a la base de datos...");
            Class.forName(dataBaseDriver);
            try (Connection connection = DriverManager.getConnection(dataBaseURL, dataBaseUser, dataBasePassword)) {
                connection.setAutoCommit(false);

                System.out.println("Desactivando restricciones...");
                disableAllTriggers(connection, schemaName);

                truncateTablesByLike(connection, schemaName, "meta\\_%", tablasExcluidas);
                truncateTablesByLike(connection, schemaName, "auth\\_%", tablasExcluidas);
                truncateTablesByName(connection, schemaName, tablasIncluidas);


                enableAllTriggers(connection, schemaName);

                connection.commit();
                System.out.println("Operación completada exitosamente.");
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
            table.enablebleAllTriggers();
        }
    }


    private void truncateTablesByLike(Connection connection, String schemaName, String like, Set<String> tablasExcluidas) {
        System.out.println("Borrando contenido de las tablas que empiezan por '" + like + "' .....");
        DatabaseSchema databaseSchema = new DatabaseSchema(connection, schemaName);
        List<Table> tables = databaseSchema.getTablesByLike(like);

        for (Table table : tables) {
            String tableName = table.getTableName();
            if (!tablasExcluidas.contains(tableName)) {
                System.out.println("Borrando contenido de la tabla:" + tableName);
                table.truncate();
            }
        }

    }

    private void truncateTablesByName(Connection connection, String schemaName, Set<String> tablasIncluidas) {
        DatabaseSchema databaseSchema = new DatabaseSchema(connection, schemaName);

        for (String tableName : tablasIncluidas) {
            Table table = databaseSchema.getTable(tableName);
            if (table!=null) {
                System.out.println("Borrando contenido de la tabla:" + tableName);
                table.truncate();
            } else {
                System.out.println("La tabla '" + tableName + "' no existe en el esquema '" + schemaName+"'");
            }
        }
    }

}


