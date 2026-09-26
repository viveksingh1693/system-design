package com.viv.location.publisher;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.viv.location.model.LocationUpdatedEvent;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Component
public class LocationEventPublisher {

    private static final String TOPIC = "location-updates";

    private final KafkaTemplate<String, LocationUpdatedEvent> kafkaTemplate;

    public void publish(LocationUpdatedEvent event) {

        kafkaTemplate.send(
                TOPIC,
                event.userId(),
                event);
    }

}
