package com.viv.location.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

class LocationUpdatedEventSerializationTest {

    @Test
    void shouldSerializeInstantInEventPayload() {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        LocationUpdatedEvent event = new LocationUpdatedEvent(
                "user-1",
                12.34,
                56.78,
                5.0,
                Instant.parse("2026-09-25T15:20:00Z")
        );

        String json = assertDoesNotThrow(() -> mapper.writeValueAsString(event));
        assertTrue(json.contains("2026-09-25T15:20:00Z"));
    }

    @Test
    void shouldConfigureKafkaConsumerToDeserializeLocationUpdatedEvents() throws IOException {
        String config = Files.readString(Path.of("src/main/resources/application.yaml"), StandardCharsets.UTF_8);

        assertTrue(config.contains("value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer"));
        assertTrue(config.contains("spring.json.trusted.packages: com.viv.location.model"));
        assertTrue(config.contains("spring.json.value.default.type: com.viv.location.model.LocationUpdatedEvent"));
    }
}
