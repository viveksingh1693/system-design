package com.viv.business.dto;

import java.time.Instant;
import java.util.UUID;

import com.viv.business.enums.LocationStatus;

public record BusinessLocationResponse(

        UUID id,

        Double latitude,

        Double longitude,

        String addressLine1,

        String addressLine2,

        String city,

        String state,

        String country,

        String postalCode,

        LocationStatus status,

        Instant createdAt,

        Instant updatedAt) {
}