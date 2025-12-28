package com.educaflow.common.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class Table {

    private final Connection connection;
    private final String tableName;

    public Table(Connection connection, String tableName) {
        this.connection = connection;
        this.tableName = tableName;
    }

    public void disableAllTriggers() {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("ALTER TABLE " + tableName + " DISABLE TRIGGER ALL;");
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void enablebleAllTriggers() {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("ALTER TABLE " + tableName + " ENABLE TRIGGER ALL;");
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void truncate() {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("TRUNCATE TABLE " + tableName + " CASCADE;");
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    public String getTableName() {
        return tableName;
    }


}
