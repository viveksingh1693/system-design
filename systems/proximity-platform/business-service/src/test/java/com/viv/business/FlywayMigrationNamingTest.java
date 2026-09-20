package com.viv.business;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

class FlywayMigrationNamingTest {

    @Test
    void flyway_migration_files_should_follow_versioned_names() throws IOException {
        Path migrationDir = Path.of("src/main/resources/db/migration");

        List<String> migrationNames = Files.list(migrationDir)
                .map(path -> path.getFileName().toString())
                .sorted()
                .collect(Collectors.toList());

        assertThat(migrationNames)
                .allMatch(name -> name.matches("V\\d+__.*\\.sql"));

        assertThat(migrationNames)
                .containsExactly(
                        "V1__create_business_category.sql",
                        "V2__create_business.sql",
                        "V3__create_business_location.sql",
                        "V4__create_outbox_event.sql");
    }
}
