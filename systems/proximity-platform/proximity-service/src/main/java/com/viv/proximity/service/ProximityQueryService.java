package com.viv.proximity.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import com.viv.proximity.dto.NearbyBusinessResponse;
import com.viv.proximity.repository.BusinessGeoRepository;
import com.viv.proximity.repository.BusinessLocationMetadataRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.redis.connection.RedisGeoCommands.GeoLocation;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProximityQueryService {

        private final BusinessGeoRepository geoRepository;
        private final BusinessLocationMetadataRepository locationMetadataRepository;

        public List<NearbyBusinessResponse> findNearby( double latitude, double longitude, double radiusMeters,int limit) {
                log.info("Finding nearby businesses: latitude={}, longitude={}, radius={}, limit={}", latitude, longitude, radiusMeters, limit);
                List<GeoResult<GeoLocation<String>>> geoResults = geoRepository.findNearby(
                                latitude,
                                longitude,
                                radiusMeters,
                                limit);

                if (geoResults.isEmpty()) {
                        return List.of();
                }

                List<NearbyBusinessResponse> businesses = new ArrayList<>(geoResults.size());

                for (GeoResult<GeoLocation<String>> result : geoResults) {

                        String locationIdValue = result.getContent().getName();

                        UUID locationId;

                        try {
                                locationId = UUID.fromString(locationIdValue);
                        } catch (IllegalArgumentException exception) {

                                log.warn(
                                                "Invalid locationId returned from GEO index. locationId={}",
                                                locationIdValue,
                                                exception);

                                continue;
                        }

                        Map<Object, Object> metadata = locationMetadataRepository.find(locationId);

                        if (metadata == null || metadata.isEmpty()) {

                                log.warn(
                                                "Location metadata not found. locationId={}",
                                                locationId);

                                continue;
                        }

                        /*
                         * A location is searchable only when BOTH the business
                         * and the location are active.
                         */
                        String businessStatus = getRequiredString(
                                        metadata,
                                        "businessStatus");

                        String locationStatus = getRequiredString(
                                        metadata,
                                        "locationStatus");

                        if (!"ACTIVE".equals(businessStatus)
                                        || !"ACTIVE".equals(locationStatus)) {

                                log.debug(
                                                "Skipping inactive location. locationId={}, businessStatus={}, locationStatus={}",
                                                locationId,
                                                businessStatus,
                                                locationStatus);

                                continue;
                        }

                        UUID businessId = UUID.fromString(
                                        getRequiredString(
                                                        metadata,
                                                        "businessId"));

                        UUID categoryId = UUID.fromString(
                                        getRequiredString(
                                                        metadata,
                                                        "categoryId"));

                        String name = getRequiredString(
                                        metadata,
                                        "name");

                        double locationLatitude = getRequiredDouble(
                                        metadata,
                                        "latitude");

                        double locationLongitude = getRequiredDouble(
                                        metadata,
                                        "longitude");

                        double distanceMeters = result.getDistance() != null
                                        ? result.getDistance().getValue()
                                        : 0.0;

                        NearbyBusinessResponse response = new NearbyBusinessResponse(
                                        businessId,
                                        locationId,
                                        categoryId,
                                        name,
                                        businessStatus,
                                        locationLatitude,
                                        locationLongitude,
                                        distanceMeters);

                        businesses.add(response);
                }

                return businesses;
        }

        private String getRequiredString(
                        Map<Object, Object> metadata,
                        String field) {

                Object value = metadata.get(field);

                if (value == null) {

                        throw new IllegalStateException(
                                        "Required location metadata field is missing: "
                                                        + field
                                                        + ", metadata="
                                                        + metadata);
                }

                return value.toString();
        }

        private double getRequiredDouble(
                        Map<Object, Object> metadata,
                        String field) {

                String value = getRequiredString(
                                metadata,
                                field);

                try {
                        return Double.parseDouble(value);
                } catch (NumberFormatException exception) {

                        throw new IllegalStateException(
                                        "Invalid numeric location metadata. "
                                                        + "field=" + field
                                                        + ", value=" + value,
                                        exception);
                }
        }
}