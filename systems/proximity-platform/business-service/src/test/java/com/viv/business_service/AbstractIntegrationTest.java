package com.viv.business_service;

import org.junit.jupiter.api.AfterAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

//@Testcontainers
@SpringBootTest
public abstract class AbstractIntegrationTest {

        @SuppressWarnings({ "resource", "deprecation" })
        static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
                        "postgis/postgis:16-3.5")
                        .withDatabaseName("business")
                        .withUsername("business")
                        .withPassword("business");

        static {
                POSTGRES.start();
        }

        @AfterAll
        static void stopPostgres() {
                POSTGRES.stop();
        }

        @DynamicPropertySource
        static void configureDatabase(
                        DynamicPropertyRegistry registry) {

                registry.add(
                                "spring.datasource.url",
                                POSTGRES::getJdbcUrl);

                registry.add(
                                "spring.datasource.username",
                                POSTGRES::getUsername);

                registry.add(
                                "spring.datasource.password",
                                POSTGRES::getPassword);

                registry.add(
                                "spring.datasource.driver-class-name",
                                POSTGRES::getDriverClassName);

                registry.add(
                                "spring.jpa.hibernate.ddl-auto",
                                () -> "validate");

                registry.add(
                                "spring.flyway.enabled",
                                () -> true);
        }
}