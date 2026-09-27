package com.viv.location.publisher;

import com.viv.location.model.LocationUpdatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LocationEventPublisher {

    private static final String BINDING_NAME = "locationUpdates-out";

    private final StreamBridge streamBridge;

    public void publish(LocationUpdatedEvent event) {

        boolean sent = streamBridge.send(
                BINDING_NAME,
                event
        );

        if (!sent) {
            throw new IllegalStateException(
                    "Failed to publish location update for user: "
                            + event.userId()
            );
        }
    }
}