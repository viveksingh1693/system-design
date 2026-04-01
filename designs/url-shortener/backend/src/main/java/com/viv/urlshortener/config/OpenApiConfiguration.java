package com.viv.urlshortener.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean
    public OpenAPI urlShortenerOpenApi(@Value("${app.shortener.base-url}") String baseUrl) {
        return new OpenAPI()
                .info(new Info()
                        .title("URL Shortener API")
                        .version("v1")
                        .description("API for creating, inspecting, disabling, and resolving short URLs.")
                        .contact(new Contact().name("Viv URL Shortener"))
                        .license(new License().name("Internal Use")))
                .addServersItem(new Server().url(baseUrl).description("Configured application base URL"));
    }
}
