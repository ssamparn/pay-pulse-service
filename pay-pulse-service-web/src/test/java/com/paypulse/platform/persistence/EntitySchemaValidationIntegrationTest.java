package com.paypulse.platform.persistence;

import com.paypulse.platform.AbstractIntegrationTest;
import com.paypulse.platform.persistence.entity.PaymentBatchEntity;
import com.paypulse.platform.persistence.entity.PaymentTransactionEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class EntitySchemaValidationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void shouldValidateEntityMappingsAgainstPostMigrationSchema() throws SQLException {
        migrateSchema();

        validateEntityColumns(PaymentBatchEntity.class);
        validateEntityColumns(PaymentTransactionEntity.class);
    }

    private void migrateSchema() {
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load()
                .migrate();
    }

    private void validateEntityColumns(Class<?> entityClass) throws SQLException {
        Table table = entityClass.getAnnotation(Table.class);
        assertNotNull(table, () -> "Missing @Table annotation on " + entityClass.getSimpleName());

        String tableName = table.name();
        assertTrue(tableExists(tableName), () -> "Missing table in database: " + tableName);

        Set<String> databaseColumns = fetchDatabaseColumns(tableName);

        for (Field field : entityClass.getDeclaredFields()) {
            Column column = field.getAnnotation(Column.class);
            if (column == null || column.name().isBlank()) {
                continue;
            }

            String mappedColumn = column.name().toLowerCase(Locale.ROOT);
            assertTrue(
                    databaseColumns.contains(mappedColumn),
                    () -> "Missing mapped column '" + mappedColumn + "' for field '" + field.getName() + "' in table '" + tableName + "'"
            );
        }
    }

    private boolean tableExists(String tableName) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             ResultSet tables = connection.getMetaData().getTables(null, schemaForMetadata(connection), tableName, new String[]{"TABLE"})) {
            return tables.next();
        }
    }

    private Set<String> fetchDatabaseColumns(String tableName) throws SQLException {
        Set<String> columns = new HashSet<>();
        try (Connection connection = dataSource.getConnection();
             ResultSet resultSet = connection.getMetaData().getColumns(null, schemaForMetadata(connection), tableName, null)) {
            while (resultSet.next()) {
                columns.add(resultSet.getString("COLUMN_NAME").toLowerCase(Locale.ROOT));
            }
        }
        return columns;
    }

    private String schemaForMetadata(Connection connection) throws SQLException {
        String schema = connection.getSchema();
        return (schema == null || schema.isBlank()) ? "public" : schema;
    }
}

