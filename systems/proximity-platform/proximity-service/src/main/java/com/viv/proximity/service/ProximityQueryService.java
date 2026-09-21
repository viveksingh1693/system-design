package com.viv.proximity.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.geo.GeoResult;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.stereotype.Service;

import com.viv.proximity.dto.NearbyBusinessResponse;
import com.viv.proximity.redis.BusinessGeoRepository;
import com.viv.proximity.redis.BusinessMetadataRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProximityQueryService {

    private final BusinessGeoRepository geoRepository;
    private final BusinessMetadataRepository metadataRepository;

    public List<NearbyBusinessResponse> findNearby(
            double latitude,
            double longitude,
            double radiusMeters,
            int limit) {

        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> geoResults =
                geoRepository.findNearby(
                        latitude,
                        longitude,
                        radiusMeters,
                        limit
                );

        List<NearbyBusinessResponse> businesses =
                new ArrayList<>();

        for (GeoResult<RedisGeoCommands.GeoLocation<String>> result
                : geoResults) {

            String businessId =
                    result.getContent().getName();

            double distanceMeters =
                    result.getDistance().getValue();

            Map<Object, Object> metadata =
                    metadataRepository.find(
                            UUID.fromString(businessId)
                    );

            if (metadata == null || metadata.isEmpty()) {
                continue;
            }

            NearbyBusinessResponse response =
                    new NearbyBusinessResponse(
                            UUID.fromString(
                                    metadata.get("businessId").toString()
                            ),
                            UUID.fromString(
                                    metadata.get("locationId").toString()
                            ),
                            UUID.fromString(
                                    metadata.get("categoryId").toString()
                            ),
                            metadata.get("name").toString(),
                            metadata.get("status").toString(),
                            Double.parseDouble(
                                    metadata.get("latitude").toString()
                            ),
                            Double.parseDouble(
                                    metadata.get("longitude").toString()
                            ),
                            distanceMeters
                    );

            businesses.add(response);
        }

        return businesses;
    }
}