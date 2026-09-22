package com.servicehub.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração central da documentação OpenAPI/Swagger do ServiceHub API.
 * Define os metadados globais exibidos no topo da interface Swagger UI,
 * disponível em /swagger-ui.html.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI serviceHubOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ServiceHub API")
                        .description("API de gerenciamento de prestadores de serviço, clientes e serviços "
                                + "oferecidos na plataforma ServiceHub. Projeto integrador da disciplina "
                                + "de Engenharia de Software — 6º Período.")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Time ServiceHub")
                                .email("api@servicehub.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")));
    }
}
