package com.prestamosrapidos.prestamos_app;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class DatabaseSchemaTest {

    @Autowired
    private DataSource dataSource;

    @Test
    public void testFlywayMigrationsApplied() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            // Check if flyway_schema_history table exists
            DatabaseMetaData metaData = connection.getMetaData();
            try (ResultSet tables = metaData.getTables(null, null, "flyway_schema_history", null)) {
                assertTrue(tables.next(), "flyway_schema_history table should exist");
            }

            // Check if our migration was applied
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '20240614193700'", 
                Integer.class);
            
            assertNotNull(count, "Should be able to query flyway_schema_history");
            assertTrue(count > 0, "Migration V20240614193700__add_mora_columns should be applied");
        }
    }

    @Test
    public void testPrestamosTableHasMoraColumns() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            
            // Check for dias_mora column
            try (ResultSet columns = metaData.getColumns(null, null, "prestamos", "dias_mora")) {
                assertTrue(columns.next(), "dias_mora column should exist in prestamos table");
                assertEquals("integer", columns.getString("TYPE_NAME").toLowerCase(), 
                    "dias_mora should be of type integer");
            }

            // Check for mora_acumulada column
            try (ResultSet columns = metaData.getColumns(null, null, "prestamos", "mora_acumulada")) {
                assertTrue(columns.next(), "mora_acumulada column should exist in prestamos table");
                assertEquals("numeric", columns.getString("TYPE_NAME").toLowerCase(), 
                    "mora_acumulada should be of type numeric");
            }

            // Check for fecha_ultimo_calculo_mora column
            try (ResultSet columns = metaData.getColumns(null, null, "prestamos", "fecha_ultimo_calculo_mora")) {
                assertTrue(columns.next(), "fecha_ultimo_calculo_mora column should exist in prestamos table");
                assertEquals("timestamp", columns.getString("TYPE_NAME").toLowerCase(), 
                    "fecha_ultimo_calculo_mora should be of type timestamp");
            }
        }
    }
}
