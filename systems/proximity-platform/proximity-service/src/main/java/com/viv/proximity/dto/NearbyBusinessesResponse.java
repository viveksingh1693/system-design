package com.viv.proximity.dto;

import java.util.List;

public record NearbyBusinessesResponse(
        double latitude,
        double longitude,
        double radiusMeters,
        List<NearbyBusinessResponse> businesses
) {
}