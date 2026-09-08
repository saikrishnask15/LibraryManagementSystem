package com.example.LibraryManagementSystem.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.List;

/**
 * Configures the OpenAPI/Swagger server URL so Swagger UI uses the HTTPS production backend.
 */
@Configuration
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
