package com.paypulse.platform.persistence;

import com.paypulse.platform.AbstractIntegrationTest;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class FlywayMigrationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void shouldApplyV1MigrationAndCreateSchemaObjects() throws SQLException {
        migrateSchema();

        assertTrue(tableExists("payment_batch"));
        assertTrue(tableExists("payment_transaction"));
        assertTrue(columnExists("payment_batch", "completed_at"));
        assertTrue(columnExists("payment_transaction", "processed_at"));
    }

    private void migrateSchema() {
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load()
                .migrate();
    }

    private boolean tableExists(String tableName) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             ResultSet tables = connection.getMetaData().getTables(null, schemaForMetadata(connection), tableName, new String[]{"TABLE"})) {
            return tables.next();
        }
    }

    private boolean columnExists(String tableName, String columnName) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             ResultSet columns = connection.getMetaData().getColumns(null, schemaForMetadata(connection), tableName, columnName)) {
            return columns.next();
        }
    }

    private String schemaForMetadata(Connection connection) throws SQLException {
        String schema = connection.getSchema();
        return (schema == null || schema.isBlank()) ? "public" : schema;
    }
}