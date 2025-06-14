package com.prestamosrapidos.prestamos_app;

import org.flywaydb.core.Flyway;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * Utilidad para reparar el estado de Flyway durante las pruebas.
 * Esta clase NO debe anotarse con @SpringBootApplication
 */
public class FlywayRepair implements CommandLineRunner {

    private final DataSource dataSource;

    public FlywayRepair(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(String... args) {
        System.out.println("Running Flyway repair...");
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .baselineOnMigrate(true)
                .validateOnMigrate(false) // Deshabilitar validación para reparación
                .load();
        
        // Ejecutar reparación para corregir el estado de migración
        flyway.repair();
        System.out.println("Flyway repair completed successfully!");
    }
}
