package com.servicehub.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração central da documentação OpenAPI/Swagger do ServiceHub API
 * (Swagger UI em /swagger-ui.html). Declara o esquema Bearer JWT: use o botão "Authorize"
 * com o accessToken devolvido por POST /api/auth/login.
 */
@Configuration
public class OpenApiConfig {

    public static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI serviceHubOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ServiceHub API")
                        .description("API de gerenciamento de prestadores de serviço, clientes, serviços e "
                                + "ordens de serviço da plataforma ServiceHub. Autenticação via JWT (Bearer). "
                                + "Projeto da disciplina de Desenvolvimento de API Back-End — Engenharia de Software.")
                        .version("v2.0.0")
                        .contact(new Contact()
                                .name("Time ServiceHub")
                                .email("api@servicehub.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Cole o accessToken retornado por POST /api/auth/login")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}
