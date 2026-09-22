package com.viv.proximity.service;

import com.viv.proximity.model.*;
import com.viv.proximity.redis.BusinessGeoRepository;
import com.viv.proximity.redis.BusinessLocationMetadataRepository;
import com.viv.proximity.redis.BusinessMetadataRepository;
import com.viv.proximity.repository.BusinessLocationIndexRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

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

                metadataRepository.updateStatus(
                                envelope.aggregateId(),
                                "ACTIVE");
        }

        private void handleBusinessDeactivated(
                        BusinessEventEnvelope envelope) {

                removeAllBusinessLocations(
                                envelope.aggregateId());

                metadataRepository.updateStatus(
                                envelope.aggregateId(),
                                "INACTIVE");
        }

        private void handleBusinessSuspended(
                        BusinessEventEnvelope envelope) {

                removeAllBusinessLocations(
                                envelope.aggregateId());

                metadataRepository.updateStatus(
                                envelope.aggregateId(),
                                "SUSPENDED");
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

                if (!"ACTIVE".equals(event.locationStatus())
                                || !"ACTIVE".equals(event.businessStatus())) {

                        geoRepository.remove(event.locationId());

                        locationMetadataRepository.updateStatus(
                                        event.locationId(),
                                        event.locationStatus());

                        return;
                }

                locationMetadataRepository.save(
                                event.businessId(),
                                event.locationId(),
                                event.categoryId(),
                                event.businessName(),
                                event.businessStatus(),
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

        private void handleLocationDeactivated(
                        BusinessEventEnvelope envelope) {

                BusinessLocationDeactivatedEvent event = convert(
                                envelope.payload(),
                                BusinessLocationDeactivatedEvent.class);

                geoRepository.remove(event.locationId());

                locationMetadataRepository.updateStatus(
                                event.locationId(),
                                "INACTIVE");

                locationIndexRepository.remove(
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

        private void removeAllBusinessLocations(UUID businessId) {

                Set<String> locationIds = locationIndexRepository.findLocationIds(businessId);

                for (String locationId : locationIds) {

                        UUID id = UUID.fromString(locationId);

                        geoRepository.remove(id);

                        locationMetadataRepository.updateStatus(
                                        id,
                                        "INACTIVE");
                }

                locationIndexRepository.delete(businessId);
        }
}