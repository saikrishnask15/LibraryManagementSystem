package com.example.LibraryManagementSystem.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.List;

/**
 * Unified OpenAPI/Swagger configuration.
 * Configures both the server URL for production backend and API documentation metadata.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Library Management System API",
                description = "Production-grade REST API with JWT authentication",
                version = "1.0.0",
                contact = @Contact(
                        name = "Sai Krishna",
                        email = "skdevexpress@gmail.com"
                )
        )
)
@SecurityScheme(
        name = "Bearer Authentication",
        description = "JWT token from /api/auth/login",
        scheme = "bearer",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER
)
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        Server prodServer = new Server();
        prodServer.setUrl("https://librarymanagementsystem-production-a51d.up.railway.app");
        prodServer.setDescription("Production (HTTPS) server");

        return new OpenAPI()
                .servers(List.of(prodServer));
    }
}
