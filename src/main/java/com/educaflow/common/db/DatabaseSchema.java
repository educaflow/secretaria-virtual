package com.educaflow.common.db;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseSchema {

    private final Connection connection;
    private final String schemaName;

    public DatabaseSchema(Connection connection, String schemaName) {
        this.connection = connection;
        this.schemaName = schemaName;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public List<Table> getTablesByLike(String like) {
        String sql="SELECT tablename FROM pg_tables WHERE schemaname = ? AND tablename LIKE ?";

        List<Table> tables = new ArrayList<>();

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, schemaName);
            preparedStatement.setString(2, like);
            ResultSet rsTables = preparedStatement.executeQuery();
            while (rsTables.next()) {
                String tableName=rsTables.getString("tablename");
                tables.add(new Table(connection, tableName));
            }
            rsTables.close();
            return tables;
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    public List<Table> getAllTables() {
        String sql="SELECT tablename FROM pg_tables WHERE schemaname = ?";

        List<Table> tables = new ArrayList<>();

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, schemaName);
            ResultSet rsTables = preparedStatement.executeQuery();
            while (rsTables.next()) {
                String tableName=rsTables.getString("tablename");
                tables.add(new Table(connection, tableName));
            }
            rsTables.close();
            return tables;
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    public Table getTable(String tableName) {
        String sql="SELECT tablename FROM pg_tables WHERE schemaname = ? AND tablename = ?";

        Table table;

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, schemaName);
            preparedStatement.setString(2, tableName);
            ResultSet rsTables = preparedStatement.executeQuery();


            if (rsTables.next()) {
                table = new Table(connection, rsTables.getString("tablename"));
            } else {
                table=null;
            }
            rsTables.close();

            return table;

        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

}
