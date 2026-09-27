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

            userGeoRepository.updateLocation(
                    event.userId(),
                    event.latitude(),
                    event.longitude()
            );

            log.info(
                    "Updated location: user={}} lat={}} lon={}}",
                    event.userId(),
                    event.latitude(),
                    event.longitude()
            );
        };
    }
}