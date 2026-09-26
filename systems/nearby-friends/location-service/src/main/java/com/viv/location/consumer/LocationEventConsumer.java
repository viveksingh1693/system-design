package com.viv.location.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.viv.location.model.LocationUpdatedEvent;
import com.viv.location.repository.UserGeoRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class LocationEventConsumer {

    private final UserGeoRepository userGeoRepository;

    @KafkaListener(
            topics = "location-updates",
            groupId = "location-processor"
    )
    public void consume(LocationUpdatedEvent event) {
        userGeoRepository.updateLocation(
                event.userId(),
                event.latitude(),
                event.longitude());
    }

}
