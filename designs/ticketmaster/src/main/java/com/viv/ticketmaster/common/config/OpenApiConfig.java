package com.viv.ticketmaster.common.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.tags.Tag;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI ticketmasterOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Ticketmaster API")
                        .version("v1")
                        .description("System design sample with catalog, inventory, booking, and payment services."))
                .addTagsItem(new Tag()
                        .name("Catalog")
                        .description("Event discovery and onboarding APIs."))
                .addTagsItem(new Tag()
                        .name("Inventory")
                        .description("Show inventory and seat availability APIs."))
                .addTagsItem(new Tag()
                        .name("Bookings")
                        .description("Ticket hold, confirmation, and cancellation APIs."));
    }

    @Bean
    public GroupedOpenApi catalogOpenApi() {
        return GroupedOpenApi.builder()
                .group("catalog")
                .pathsToMatch("/events/**")
                .build();
    }

    @Bean
    public GroupedOpenApi inventoryOpenApi() {
        return GroupedOpenApi.builder()
                .group("inventory")
                .pathsToMatch("/shows/**")
                .build();
    }

    @Bean
    public GroupedOpenApi bookingsOpenApi() {
        return GroupedOpenApi.builder()
                .group("bookings")
                .pathsToMatch("/bookings/**")
                .build();
    }
}
