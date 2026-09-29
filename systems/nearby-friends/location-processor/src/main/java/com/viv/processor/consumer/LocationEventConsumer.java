package com.viv.processor.consumer;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.viv.processor.model.LocationUpdatedEvent;
import com.viv.processor.repository.UserGeoRepository;

import lombok.extern.slf4j.Slf4j;

import java.util.function.Consumer;

@Slf4j
@Configuration
public class LocationEventConsumer {

    @Bean
    public Consumer<LocationUpdatedEvent> locationUpdates(
            UserGeoRepository userGeoRepository) {

        return event -> {

            boolean updated = userGeoRepository.updateLocation(event);

            if (updated) {
                log.info("Location updated: user={}, timestamp={}", event.userId(), event.timestamp());
            } else {
                log.info("Stale location ignored: user={}, timestamp={}", event.userId(), event.timestamp());
            }
        };
    }
}