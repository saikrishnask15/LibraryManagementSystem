package com.example.LibraryManagementSystem.config.openAPI;


import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Library Management System API",
                description = "Production-grade REST API with JWT authentication",
                version = "1.0.0",
                contact = @Contact(
                        name = " Sai Krishna",
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
}
