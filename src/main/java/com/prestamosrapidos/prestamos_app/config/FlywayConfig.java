package com.prestamosrapidos.prestamos_app.config;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.util.Arrays;

@Configuration
public class FlywayConfig {

    private static final Logger logger = LoggerFactory.getLogger(FlywayConfig.class);

    @Autowired
    private Environment env;

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            try {
                // Validar configuraciones
                logger.info("Validando configuración de Flyway...");
                String locations = Arrays.stream(flyway.getConfiguration().getLocations())
                    .map(location -> location.getPath())
                    .collect(java.util.stream.Collectors.joining(", "));
                logger.info("Ubicaciones de migración: {}", locations);
                
                // Ejecutar reparación si es necesario
                logger.info("Ejecutando reparación de Flyway...");
                flyway.repair();
                logger.info("Reparación de Flyway completada");
                
                // Ejecutar migraciones
                logger.info("Iniciando migraciones de Flyway...");
                MigrateResult result = flyway.migrate();
                
                // Mostrar resultados
                logger.info("Migración completada exitosamente");
                logger.info("Migraciones aplicadas: {}", result.migrationsExecuted);
                if (result.migrationsExecuted > 0) {
                    logger.info("Migraciones aplicadas: {}", 
                        Arrays.toString(result.migrations.stream()
                            .map(m -> m.filepath + " (" + m.version + ")")
                            .toArray()));
                }
                
            } catch (Exception e) {
                logger.error("Error durante la migración de Flyway: {}", e.getMessage(), e);
                throw new RuntimeException("Error en la migración de base de datos: " + e.getMessage(), e);
            }
        };
    }

    @Bean(initMethod = "migrate")
    public Flyway flyway(DataSource dataSource) {
        logger.info("Inicializando Flyway...");
        
        try {
            Flyway flyway = Flyway.configure()
                    .dataSource(dataSource)
                    .locations("classpath:db/migration")
                    .baselineOnMigrate(true)
                    .baselineVersion("0")
                    .validateOnMigrate(false)
                    .validateMigrationNaming(true)
                    .outOfOrder(false)
                    .cleanDisabled(true)  // Deshabilitar clean para evitar eliminación accidental de datos
                    .sqlMigrationPrefix("V")
                    .sqlMigrationSeparator("__")
                    .sqlMigrationSuffixes(".sql")
                    .placeholderReplacement(false)
                    .connectRetries(10)
                    .loggers("slf4j")
                    .load();
            
            logger.info("Flyway inicializado correctamente");
            return flyway;
            
        } catch (Exception e) {
            logger.error("Error al inicializar Flyway: {}", e.getMessage(), e);
            throw new RuntimeException("No se pudo inicializar Flyway", e);
        }
    }
}
