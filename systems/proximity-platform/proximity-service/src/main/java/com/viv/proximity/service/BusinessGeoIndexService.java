package com.viv.proximity.service;

import com.viv.proximity.model.*;
import com.viv.proximity.redis.BusinessGeoRepository;
import com.viv.proximity.redis.BusinessMetadataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class BusinessGeoIndexService {

        private final ObjectMapper objectMapper;
        private final BusinessGeoRepository geoRepository;
        private final BusinessMetadataRepository metadataRepository;

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

                metadataRepository.updateStatus(
                                envelope.aggregateId(),
                                "DELETED");

                geoRepository.remove(
                                envelope.aggregateId());
        }

        private void handleBusinessSuspended(
                        BusinessEventEnvelope envelope) {

                metadataRepository.updateStatus(
                                envelope.aggregateId(),
                                "SUSPENDED");

                geoRepository.remove(
                                envelope.aggregateId());
        }

        private void handleLocationCreated(
                        BusinessEventEnvelope envelope) {

                BusinessLocationCreatedEvent event = convert(
                                envelope.payload(),
                                BusinessLocationCreatedEvent.class);

                log.info(
                                "Processing BUSINESS_LOCATION_CREATED. " +
                                                "businessId={}, locationId={}, categoryId={}, " +
                                                "businessName={}, businessStatus={}, locationStatus={}, " +
                                                "latitude={}, longitude={}",
                                event.businessId(),
                                event.locationId(),
                                event.categoryId(),
                                event.businessName(),
                                event.businessStatus(),
                                event.locationStatus(),
                                event.latitude(),
                                event.longitude());

                if (!"ACTIVE".equals(event.locationStatus())) {

                        log.info(
                                        "Ignoring location because locationStatus is not ACTIVE. " +
                                                        "businessId={}, locationId={}, locationStatus={}",
                                        event.businessId(),
                                        event.locationId(),
                                        event.locationStatus());

                        return;
                }

                if (!"ACTIVE".equals(event.businessStatus())) {

                        log.info(
                                        "Ignoring location because businessStatus is not ACTIVE. " +
                                                        "businessId={}, businessStatus={}",
                                        event.businessId(),
                                        event.businessStatus());

                        return;
                }

                metadataRepository.save(
                                event.businessId(),
                                event.locationId(),
                                event.categoryId(),
                                event.businessName(),
                                event.businessStatus(),
                                event.latitude(),
                                event.longitude());

                geoRepository.add(
                                event.businessId(),
                                event.latitude(),
                                event.longitude());

                log.info(
                                "Business location added to proximity index. " +
                                                "businessId={}, locationId={}, latitude={}, longitude={}",
                                event.businessId(),
                                event.locationId(),
                                event.latitude(),
                                event.longitude());
        }

        private void handleLocationUpdated(
                        BusinessEventEnvelope envelope) {

                BusinessLocationUpdatedEvent event = convert(
                                envelope.payload(),
                                BusinessLocationUpdatedEvent.class);

                if (!"ACTIVE".equals(event.locationStatus())
                                || !"ACTIVE".equals(event.businessStatus())) {

                        geoRepository.remove(
                                        event.businessId());

                        metadataRepository.updateStatus(
                                        event.businessId(),
                                        event.businessStatus());

                        return;
                }

                metadataRepository.save(
                                event.businessId(),
                                event.locationId(),
                                event.categoryId(),
                                event.businessName(),
                                event.businessStatus(),
                                event.latitude(),
                                event.longitude());

                geoRepository.add(
                                event.businessId(),
                                event.latitude(),
                                event.longitude());
        }

        private void handleLocationDeactivated(
                        BusinessEventEnvelope envelope) {

                geoRepository.remove(envelope.aggregateId());
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
}