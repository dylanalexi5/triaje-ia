package com.campuslands.tickets.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI documentacionApi() {
        return new OpenAPI().info(new Info()
                .title("Triaje IA · API de tickets de soporte")
                .version("0.1.0")
                .description("""
                        Registro y gestión de tickets de soporte técnico interno. Cada ticket nuevo se \
                        clasifica automáticamente (categoría, prioridad, resumen y etiquetas) con un \
                        modelo de lenguaje; si el proveedor de IA no está disponible, se usa un \
                        clasificador por reglas y el ticket queda marcado con `origenAnalisis: REGLAS`.

                        Usuarios de demostración (contraseña `Demo2026!`): 1 ADMIN, 2 y 3 SOPORTE, \
                        4 y 5 USUARIO."""));
    }
}
