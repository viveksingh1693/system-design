package com.viv.location.model;

import java.time.Instant;

public record LocationUpdatedEvent(
        String userId,
        double latitude,
        double longitude,
        double accuracyMeters,
        Instant timestamp
) {

}
