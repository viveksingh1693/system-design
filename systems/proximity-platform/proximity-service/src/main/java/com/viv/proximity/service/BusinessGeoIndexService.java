package com.viv.proximity.service;

import com.viv.proximity.model.*;
import com.viv.proximity.repository.BusinessGeoRepository;
import com.viv.proximity.repository.BusinessLocationIndexRepository;
import com.viv.proximity.repository.BusinessLocationMetadataRepository;
import com.viv.proximity.repository.BusinessMetadataRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class BusinessGeoIndexService {

        private final ObjectMapper objectMapper;
        private final BusinessGeoRepository geoRepository;
        private final BusinessMetadataRepository metadataRepository;
        private final BusinessLocationMetadataRepository locationMetadataRepository;
        private final BusinessLocationIndexRepository locationIndexRepository;

        public void handle(
                        BusinessEventEnvelope envelope) {

                switch (envelope.eventType()) {

                        case "BUSINESS_CREATED" ->
                                handleBusinessCreated(envelope);

                        case "BUSINESS_UPDATED" ->
                                handleBusinessUpdated(envelope);

                        case "BUSINESS_ACTIVATED" ->
                                handleBusinessActivated(envelope);

                        case "BUSINESS_DEACTIVATED" ->
                                handleBusinessDeactivated(envelope);

                        case "BUSINESS_SUSPENDED" ->
                                handleBusinessSuspended(envelope);

                        case "BUSINESS_LOCATION_CREATED" ->
                                handleLocationCreated(envelope);

                        case "BUSINESS_LOCATION_UPDATED" ->
                                handleLocationUpdated(envelope);

                        case "BUSINESS_LOCATION_DEACTIVATED" ->
                                handleLocationDeactivated(envelope);

                        default ->
                                log.debug(
                                                "Ignoring unsupported event type={}",
                                                envelope.eventType());
                }
        }

        private void handleBusinessCreated(
                        BusinessEventEnvelope envelope) {

                BusinessCreatedEvent event = convert(
                                envelope.payload(),
                                BusinessCreatedEvent.class);

                metadataRepository.updateStatus(
                                event.businessId(),
                                event.status());
        }

        private void handleBusinessUpdated(
                        BusinessEventEnvelope envelope) {

                BusinessUpdatedEvent event = convert(
                                envelope.payload(),
                                BusinessUpdatedEvent.class);

                metadataRepository.updateStatus(
                                event.businessId(),
                                event.status());
        }

        private void handleBusinessActivated(
                        BusinessEventEnvelope envelope) {

                UUID businessId = envelope.aggregateId();

                metadataRepository.updateStatus(
                                businessId,
                                "ACTIVE");

                restoreActiveBusinessLocations(businessId);

                log.info(
                                "Business activated and active locations restored. businessId={}",
                                businessId);
        }

        private void handleBusinessDeactivated(
                        BusinessEventEnvelope envelope) {

                UUID businessId = envelope.aggregateId();

                metadataRepository.updateStatus(
                                businessId,
                                "INACTIVE");

                updateBusinessLocationsStatus(
                                businessId,
                                "INACTIVE");

                removeAllBusinessLocationsFromGeo(
                                businessId);

                log.info(
                                "Business deactivated and locations removed from GEO. businessId={}",
                                businessId);
        }

        private void handleBusinessSuspended(
                        BusinessEventEnvelope envelope) {

                UUID businessId = envelope.aggregateId();

                metadataRepository.updateStatus(
                                businessId,
                                "SUSPENDED");

                updateBusinessLocationsStatus(
                                businessId,
                                "SUSPENDED");

                removeAllBusinessLocationsFromGeo(
                                businessId);

                log.info(
                                "Business suspended and locations removed from GEO. businessId={}",
                                businessId);
        }

        private void handleLocationCreated(
                        BusinessEventEnvelope envelope) {

                BusinessLocationCreatedEvent event = convert(
                                envelope.payload(),
                                BusinessLocationCreatedEvent.class);

                if (!"ACTIVE".equals(event.locationStatus())) {
                        return;
                }

                if (!"ACTIVE".equals(event.businessStatus())) {
                        return;
                }

                locationMetadataRepository.save(
                                event.businessId(),
                                event.locationId(),
                                event.categoryId(),
                                event.businessName(),
                                event.businessStatus(),
                                event.locationStatus(),
                                event.latitude(),
                                event.longitude());

                geoRepository.add(
                                event.locationId(),
                                event.latitude(),
                                event.longitude());

                locationIndexRepository.add(
                                event.businessId(),
                                event.locationId());
        }

        private void handleLocationUpdated(
                        BusinessEventEnvelope envelope) {

                BusinessLocationUpdatedEvent event = convert(
                                envelope.payload(),
                                BusinessLocationUpdatedEvent.class);

                locationMetadataRepository.save(
                                event.businessId(),
                                event.locationId(),
                                event.categoryId(),
                                event.businessName(),
                                event.businessStatus(),
                                event.locationStatus(),
                                event.latitude(),
                                event.longitude());

                locationIndexRepository.add(
                                event.businessId(),
                                event.locationId());

                if (!"ACTIVE".equals(event.businessStatus())
                                || !"ACTIVE".equals(event.locationStatus())) {

                        geoRepository.remove(
                                        event.locationId());

                        return;
                }

                geoRepository.add(
                                event.locationId(),
                                event.latitude(),
                                event.longitude());
        }

        private void handleLocationDeactivated(
                        BusinessEventEnvelope envelope) {

                BusinessLocationDeactivatedEvent event = convert(
                                envelope.payload(),
                                BusinessLocationDeactivatedEvent.class);

                geoRepository.remove(
                                event.locationId());

                locationMetadataRepository.updateLocationStatus(
                                event.locationId(),
                                "INACTIVE");

                log.info(
                                "Location removed from GEO and marked inactive. " +
                                                "businessId={}, locationId={}",
                                event.businessId(),
                                event.locationId());
        }

        private <T> T convert(
                        JsonNode payload,
                        Class<T> type) {

                try {

                        return objectMapper.treeToValue(
                                        payload,
                                        type);

                } catch (Exception e) {

                        throw new IllegalArgumentException(
                                        "Unable to deserialize business event payload",
                                        e);
                }
        }

        // private void removeAllBusinessLocations(UUID businessId) {

        //         Set<String> locationIds = locationIndexRepository.findLocationIds(businessId);

        //         for (String locationIdValue : locationIds) {

        //                 UUID locationId = UUID.fromString(locationIdValue);

        //                 geoRepository.remove(locationId);

        //                 locationMetadataRepository.updateLocationStatus(
        //                                 locationId,
        //                                 "INACTIVE");
        //         }
        // }

        private void restoreActiveBusinessLocations(
                        UUID businessId) {

                Set<String> locationIds = locationIndexRepository.findLocationIds(businessId);

                if (locationIds.isEmpty()) {

                        log.debug(
                                        "No locations found while restoring business. businessId={}",
                                        businessId);

                        return;
                }

                for (String locationIdValue : locationIds) {

                        UUID locationId;

                        try {
                                locationId = UUID.fromString(locationIdValue);

                        } catch (IllegalArgumentException exception) {

                                log.warn(
                                                "Invalid locationId found in business location index. " +
                                                                "businessId={}, locationId={}",
                                                businessId,
                                                locationIdValue,
                                                exception);

                                continue;
                        }

                        Map<Object, Object> metadata = locationMetadataRepository.find(locationId);

                        if (metadata == null || metadata.isEmpty()) {

                                log.warn(
                                                "Location metadata not found while restoring location. " +
                                                                "businessId={}, locationId={}",
                                                businessId,
                                                locationId);

                                continue;
                        }

                        String businessStatus = String.valueOf(
                                        metadata.get("businessStatus"));

                        String locationStatus = String.valueOf(
                                        metadata.get("locationStatus"));

                        /*
                         * A location can be restored only when:
                         *
                         * 1. Business is ACTIVE
                         * 2. Location is ACTIVE
                         *
                         * Example:
                         *
                         * Business ACTIVE + Location ACTIVE
                         * -> restore GEO
                         *
                         * Business ACTIVE + Location INACTIVE
                         * -> do NOT restore
                         *
                         * Business SUSPENDED + Location ACTIVE
                         * -> do NOT restore
                         */
                        if (!"ACTIVE".equals(businessStatus)
                                        || !"ACTIVE".equals(locationStatus)) {

                                log.debug(
                                                "Skipping inactive location during business activation. " +
                                                                "businessId={}, locationId={}, businessStatus={}, locationStatus={}",
                                                businessId,
                                                locationId,
                                                businessStatus,
                                                locationStatus);

                                continue;
                        }

                        Object latitudeValue = metadata.get("latitude");

                        Object longitudeValue = metadata.get("longitude");

                        if (latitudeValue == null
                                        || longitudeValue == null) {

                                log.warn(
                                                "Location coordinates missing while restoring location. " +
                                                                "businessId={}, locationId={}",
                                                businessId,
                                                locationId);

                                continue;
                        }

                        double latitude;
                        double longitude;

                        try {

                                latitude = Double.parseDouble(
                                                String.valueOf(latitudeValue));

                                longitude = Double.parseDouble(
                                                String.valueOf(longitudeValue));

                        } catch (NumberFormatException exception) {

                                log.warn(
                                                "Invalid location coordinates while restoring location. " +
                                                                "businessId={}, locationId={}, latitude={}, longitude={}",
                                                businessId,
                                                locationId,
                                                latitudeValue,
                                                longitudeValue,
                                                exception);

                                continue;
                        }

                        geoRepository.add(
                                        locationId,
                                        latitude,
                                        longitude);

                        log.info(
                                        "Restored active location to GEO. " +
                                                        "businessId={}, locationId={}, latitude={}, longitude={}",
                                        businessId,
                                        locationId,
                                        latitude,
                                        longitude);
                }
        }

        private void updateBusinessLocationsStatus(
                        UUID businessId,
                        String businessStatus) {

                Set<String> locationIds = locationIndexRepository.findLocationIds(businessId);

                for (String locationIdValue : locationIds) {

                        UUID locationId = UUID.fromString(locationIdValue);

                        locationMetadataRepository.updateBusinessStatus(
                                        locationId,
                                        businessStatus);
                }
        }

        private void removeAllBusinessLocationsFromGeo(
                        UUID businessId) {

                Set<String> locationIds = locationIndexRepository.findLocationIds(businessId);

                for (String locationIdValue : locationIds) {

                        UUID locationId = UUID.fromString(locationIdValue);

                        geoRepository.remove(locationId);
                }
        }
}