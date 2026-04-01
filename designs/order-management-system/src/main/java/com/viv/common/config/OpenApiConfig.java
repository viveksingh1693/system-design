package com.viv.common.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.tags.Tag;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI orderManagementOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Order Management System API")
                        .version("v1")
                        .description("Spring Boot API with separate MySQL and PostgreSQL datasources and Hibernate caching."))
                .addTagsItem(new Tag()
                        .name("Users")
                        .description("User APIs backed by the MySQL datasource."))
                .addTagsItem(new Tag()
                        .name("Orders")
                        .description("Order APIs backed by the PostgreSQL datasource."));
    }

    @Bean
    public GroupedOpenApi usersOpenApi() {
        return GroupedOpenApi.builder()
                .group("users")
                .pathsToMatch("/users/**")
                .build();
    }

    @Bean
    public GroupedOpenApi ordersOpenApi() {
        return GroupedOpenApi.builder()
                .group("orders")
                .pathsToMatch("/orders/**")
                .build();
    }
}
