package com.viv.proximity.dto;

import java.util.UUID;

public record NearbyBusinessResponse(
        UUID businessId,
        UUID locationId,
        UUID categoryId,
        String name,
        String status,
        double latitude,
        double longitude,
        double distanceMeters
) {
}