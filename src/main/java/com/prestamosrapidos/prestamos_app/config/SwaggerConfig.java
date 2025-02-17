package com.prestamosrapidos.prestamos_app.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API de Préstamos Rápidos")
                        .version("1.0")
                        .description("Documentación interactiva de la API de Préstamos Rápidos")
                        .license(new License().name("Apache 2.0").url("https://springdoc.org")));
    }
}
