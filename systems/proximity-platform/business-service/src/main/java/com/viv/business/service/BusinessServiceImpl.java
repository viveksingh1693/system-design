package com.viv.business.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.viv.business.dto.BusinessLocationResponse;
import com.viv.business.dto.BusinessResponse;
import com.viv.business.dto.CreateBusinessLocationRequest;
import com.viv.business.dto.CreateBusinessRequest;
import com.viv.business.dto.UpdateBusinessLocationRequest;
import com.viv.business.dto.UpdateBusinessRequest;
import com.viv.business.entity.Business;
import com.viv.business.entity.BusinessCategory;
import com.viv.business.entity.BusinessLocation;
import com.viv.business.enums.BusinessStatus;
import com.viv.business.enums.LocationStatus;
import com.viv.business.event.BusinessCreatedEvent;
import com.viv.business.event.BusinessEventType;
import com.viv.business.event.BusinessLocationCreatedEvent;
import com.viv.business.event.BusinessLocationDeactivatedEvent;
import com.viv.business.event.BusinessLocationUpdatedEvent;
import com.viv.business.event.BusinessUpdatedEvent;
import com.viv.business.event.LocationChange;
import com.viv.business.exception.BusinessCategoryNotFoundException;
import com.viv.business.exception.BusinessNotFoundException;
import com.viv.business.repository.BusinessCategoryRepository;
import com.viv.business.repository.BusinessRepository;

import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class BusinessServiceImpl implements BusinessService {

        private final OutboxService outboxService;
        private final BusinessRepository businessRepository;
        private final BusinessCategoryRepository businessCategoryRepository;

        // =========================================================
        // CREATE
        // =========================================================

        @Observed(name = "business.create", contextualName = "create-business")
        @Transactional
        @Override
        public BusinessResponse create(
                        CreateBusinessRequest request) {

                log.info(
                                "Creating business with request: {}",
                                request);

                BusinessCategory category = businessCategoryRepository
                                .findById(request.categoryId())
                                .orElseThrow(
                                                () -> new BusinessCategoryNotFoundException(
                                                                "Business category not found with ID: "
                                                                                + request.categoryId()));

                Business business = Business.builder()
                                .name(
                                                normalizeName(
                                                                request.name()))
                                .description(
                                                normalizeDescription(
                                                                request.description()))
                                .category(category)
                                .status(BusinessStatus.ACTIVE)
                                .locations(new ArrayList<>())
                                .build();

                for (CreateBusinessLocationRequest locationRequest : request.locations()) {

                        BusinessLocation location = createLocation(locationRequest);

                        business.addLocation(location);
                }

                Business savedBusiness = businessRepository.save(business);

                log.info(
                                "Created business with id: {} for categoryId: {}",
                                savedBusiness.getId(),
                                savedBusiness.getCategory().getId());

                /*
                 * ---------------------------------------------------------
                 * BUSINESS_CREATED
                 * ---------------------------------------------------------
                 */

                BusinessCreatedEvent businessEvent = new BusinessCreatedEvent(
                                savedBusiness.getId(),
                                savedBusiness.getCategory().getId(),
                                savedBusiness.getName(),
                                savedBusiness.getStatus().name(),
                                Instant.now());

                outboxService.save(
                                "BUSINESS",
                                savedBusiness.getId(),
                                BusinessEventType.BUSINESS_CREATED.name(),
                                businessEvent);

                /*
                 * ---------------------------------------------------------
                 * BUSINESS_LOCATION_CREATED
                 * ---------------------------------------------------------
                 *
                 * One event per location.
                 *
                 * aggregateId = businessId
                 * locationId is part of the event payload.
                 * ---------------------------------------------------------
                 */

                for (BusinessLocation location : savedBusiness.getLocations()) {

                        BusinessLocationCreatedEvent locationEvent = new BusinessLocationCreatedEvent(
                                        savedBusiness.getId(),
                                        location.getId(),
                                        savedBusiness.getCategory().getId(),
                                        savedBusiness.getName(),
                                        savedBusiness.getStatus().name(),
                                        location.getLatitude(),
                                        location.getLongitude(),
                                        location.getStatus().name(),
                                        Instant.now());

                        outboxService.save(
                                        "BUSINESS",
                                        savedBusiness.getId(),
                                        BusinessEventType.BUSINESS_LOCATION_CREATED
                                                        .name(),
                                        locationEvent);

                        log.info(
                                        "Created BUSINESS_LOCATION_CREATED event. " +
                                                        "businessId={}, locationId={}",
                                        savedBusiness.getId(),
                                        location.getId());
                }

                return toResponse(savedBusiness);
        }

        // =========================================================
        // GET
        // =========================================================

        @Observed(name = "business.get", contextualName = "get-business")
        @Override
        @Transactional(readOnly = true)
        public BusinessResponse getById(UUID id) {

                log.info(
                                "Fetching business by id: {}",
                                id);

                Business business = businessRepository
                                .findById(id)
                                .orElseThrow(
                                                () -> new BusinessNotFoundException(
                                                                "Business not found with ID: "
                                                                                + id));

                return toResponse(business);
        }

        // =========================================================
        // UPDATE
        // =========================================================

        @Observed(name = "business.update", contextualName = "update-business")
        @Transactional
        @Override
        public BusinessResponse update(
                        UUID id,
                        UpdateBusinessRequest request) {

                log.info(
                                "Updating business id: {} with request: {}",
                                id,
                                request);

                Business business = getBusiness(id);

                /*
                 * ---------------------------------------------------------
                 * Update business fields
                 * ---------------------------------------------------------
                 */

                business.setName(
                                normalizeName(
                                                request.name()));

                business.setDescription(
                                normalizeDescription(
                                                request.description()));

                /*
                 * ---------------------------------------------------------
                 * Update locations
                 * ---------------------------------------------------------
                 */

                List<LocationChange> locationChanges = List.of();

                if (request.locations() != null) {

                        locationChanges = updateLocations(
                                        business,
                                        request.locations());
                }

                /*
                 * ---------------------------------------------------------
                 * BUSINESS_UPDATED
                 * ---------------------------------------------------------
                 */

                BusinessUpdatedEvent businessEvent = new BusinessUpdatedEvent(
                                business.getId(),
                                business.getCategory().getId(),
                                business.getName(),
                                business.getStatus().name(),
                                Instant.now());

                outboxService.save(
                                "BUSINESS",
                                business.getId(),
                                BusinessEventType.BUSINESS_UPDATED.name(),
                                businessEvent);

                log.info(
                                "Created BUSINESS_UPDATED event. businessId={}",
                                business.getId());

                /*
                 * ---------------------------------------------------------
                 * LOCATION EVENTS
                 * ---------------------------------------------------------
                 */

                for (LocationChange change : locationChanges) {

                        BusinessLocation location = change.location();

                        if (change.created()) {

                                /*
                                 * -------------------------------------------------
                                 * BUSINESS_LOCATION_CREATED
                                 * -------------------------------------------------
                                 */

                                BusinessLocationCreatedEvent event = new BusinessLocationCreatedEvent(
                                                business.getId(),
                                                location.getId(),
                                                business.getCategory().getId(),
                                                business.getName(),
                                                business.getStatus().name(),
                                                location.getLatitude(),
                                                location.getLongitude(),
                                                location.getStatus().name(),
                                                Instant.now());

                                outboxService.save(
                                                "BUSINESS",
                                                business.getId(),
                                                BusinessEventType.BUSINESS_LOCATION_CREATED
                                                                .name(),
                                                event);

                                log.info(
                                                "Created BUSINESS_LOCATION_CREATED event. " +
                                                                "businessId={}, locationId={}",
                                                business.getId(),
                                                location.getId());

                        } else {

                                /*
                                 * -------------------------------------------------
                                 * BUSINESS_LOCATION_UPDATED
                                 * -------------------------------------------------
                                 */

                                BusinessLocationUpdatedEvent event = new BusinessLocationUpdatedEvent(
                                                business.getId(),
                                                location.getId(),
                                                business.getCategory().getId(),
                                                business.getName(),
                                                business.getStatus().name(),
                                                location.getLatitude(),
                                                location.getLongitude(),
                                                location.getStatus().name(),
                                                Instant.now());

                                outboxService.save(
                                                "BUSINESS",
                                                business.getId(),
                                                BusinessEventType.BUSINESS_LOCATION_UPDATED
                                                                .name(),
                                                event);

                                log.info(
                                                "Created BUSINESS_LOCATION_UPDATED event. " +
                                                                "businessId={}, locationId={}",
                                                business.getId(),
                                                location.getId());
                        }
                }

                log.info(
                                "Updated business id: {}",
                                id);

                return toResponse(business);
        }

        // =========================================================
        // DEACTIVATE
        // =========================================================

        @Observed(name = "business.deactivate", contextualName = "deactivate-business")
        @Transactional
        @Override
        public void deactivate(UUID id) {

                log.info(
                                "Deactivating business id: {}",
                                id);

                Business business = getBusiness(id);

                business.setStatus(
                                BusinessStatus.INACTIVE);

                /*
                 * BUSINESS_DEACTIVATED
                 *
                 * Proximity service only needs aggregateId/status.
                 * We use BusinessUpdatedEvent as the payload because
                 * the consumer does not deserialize the payload for this
                 * event type.
                 */

                BusinessUpdatedEvent event = new BusinessUpdatedEvent(
                                business.getId(),
                                business.getCategory().getId(),
                                business.getName(),
                                business.getStatus().name(),
                                Instant.now());

                outboxService.save(
                                "BUSINESS",
                                business.getId(),
                                BusinessEventType.BUSINESS_DEACTIVATED.name(),
                                event);

                log.info(
                                "Business id: {} deactivated. " +
                                                "BUSINESS_DEACTIVATED event created.",
                                id);
        }

        // =========================================================
        // SUSPEND
        // =========================================================

        @Observed(name = "business.suspend", contextualName = "suspend-business")
        @Transactional
        @Override
        public void suspend(UUID id) {

                log.info(
                                "Suspending business id: {}",
                                id);

                Business business = getBusiness(id);

                business.setStatus(
                                BusinessStatus.SUSPENDED);

                /*
                 * BUSINESS_SUSPENDED
                 */

                BusinessUpdatedEvent event = new BusinessUpdatedEvent(
                                business.getId(),
                                business.getCategory().getId(),
                                business.getName(),
                                business.getStatus().name(),
                                Instant.now());

                outboxService.save(
                                "BUSINESS",
                                business.getId(),
                                BusinessEventType.BUSINESS_SUSPENDED.name(),
                                event);

                log.info(
                                "Business id: {} suspended. " +
                                                "BUSINESS_SUSPENDED event created.",
                                id);
        }

        // =========================================================
        // ACTIVATE
        // =========================================================

        @Observed(name = "business.activate", contextualName = "activate-business")
        @Transactional
        @Override
        public void activate(UUID id) {

                log.info(
                                "Activating business id: {}",
                                id);

                Business business = getBusiness(id);

                business.setStatus(
                                BusinessStatus.ACTIVE);

                /*
                 * BUSINESS_ACTIVATED
                 */

                BusinessUpdatedEvent event = new BusinessUpdatedEvent(
                                business.getId(),
                                business.getCategory().getId(),
                                business.getName(),
                                business.getStatus().name(),
                                Instant.now());

                outboxService.save(
                                "BUSINESS",
                                business.getId(),
                                BusinessEventType.BUSINESS_ACTIVATED.name(),
                                event);

                log.info(
                                "Business id: {} activated. " +
                                                "BUSINESS_ACTIVATED event created.",
                                id);
        }

        // =========================================================
        // GET BUSINESS
        // =========================================================

        private Business getBusiness(UUID id) {

                return businessRepository
                                .findById(id)
                                .orElseThrow(
                                                () -> new BusinessNotFoundException(
                                                                "Business not found with ID: "
                                                                                + id));
        }

        // =========================================================
        // CREATE LOCATION
        // =========================================================

        private BusinessLocation createLocation(
                        CreateBusinessLocationRequest request) {

                return BusinessLocation.builder()
                                .latitude(
                                                request.latitude())
                                .longitude(
                                                request.longitude())
                                .addressLine1(
                                                normalize(
                                                                request.addressLine1()))
                                .addressLine2(
                                                normalize(
                                                                request.addressLine2()))
                                .city(
                                                normalize(
                                                                request.city()))
                                .state(
                                                normalize(
                                                                request.state()))
                                .country(
                                                normalize(
                                                                request.country()))
                                .postalCode(
                                                normalize(
                                                                request.postalCode()))
                                .status(LocationStatus.ACTIVE)
                                .build();
        }

        // =========================================================
        // UPDATE LOCATIONS
        // =========================================================

        private List<LocationChange> updateLocations(
                        Business business,
                        List<UpdateBusinessLocationRequest> requests) {

                List<LocationChange> changes = new ArrayList<>();

                for (UpdateBusinessLocationRequest request : requests) {

                        /*
                         * -----------------------------------------------------
                         * NEW LOCATION
                         * -----------------------------------------------------
                         */

                        if (request.id() == null) {

                                BusinessLocation newLocation = BusinessLocation.builder()
                                                .latitude(
                                                                request.latitude())
                                                .longitude(
                                                                request.longitude())
                                                .addressLine1(
                                                                normalize(
                                                                                request.addressLine1()))
                                                .addressLine2(
                                                                normalize(
                                                                                request.addressLine2()))
                                                .city(
                                                                normalize(
                                                                                request.city()))
                                                .state(
                                                                normalize(
                                                                                request.state()))
                                                .country(
                                                                normalize(
                                                                                request.country()))
                                                .postalCode(
                                                                normalize(
                                                                                request.postalCode()))
                                                .status(
                                                                LocationStatus.ACTIVE)
                                                .build();

                                business.addLocation(
                                                newLocation);

                                changes.add(
                                                new LocationChange(
                                                                newLocation,
                                                                true));

                                continue;
                        }

                        /*
                         * -----------------------------------------------------
                         * EXISTING LOCATION
                         * -----------------------------------------------------
                         */

                        BusinessLocation location = findLocation(
                                        business,
                                        request.id());

                        location.setLatitude(
                                        request.latitude());

                        location.setLongitude(
                                        request.longitude());

                        location.setAddressLine1(
                                        normalize(
                                                        request.addressLine1()));

                        location.setAddressLine2(
                                        normalize(
                                                        request.addressLine2()));

                        location.setCity(
                                        normalize(
                                                        request.city()));

                        location.setState(
                                        normalize(
                                                        request.state()));

                        location.setCountry(
                                        normalize(
                                                        request.country()));

                        location.setPostalCode(
                                        normalize(
                                                        request.postalCode()));

                        changes.add(
                                        new LocationChange(
                                                        location,
                                                        false));
                }

                return changes;
        }

        // =========================================================
        // FIND LOCATION
        // =========================================================

        private BusinessLocation findLocation(
                        Business business,
                        UUID locationId) {

                return business.getLocations()
                                .stream()
                                .filter(
                                                location -> location.getId()
                                                                .equals(locationId))
                                .findFirst()
                                .orElseThrow(
                                                () -> new IllegalArgumentException(
                                                                "Location does not belong to business: "
                                                                                + locationId));
        }

        // =========================================================
        // RESPONSE MAPPING
        // =========================================================

        private BusinessResponse toResponse(
                        Business business) {

                List<BusinessLocationResponse> locations = business.getLocations()
                                .stream()
                                .map(
                                                this::toLocationResponse)
                                .toList();

                return new BusinessResponse(
                                business.getId(),
                                business.getName(),
                                business.getDescription(),
                                business.getCategory().getId(),
                                business.getStatus(),
                                business.getVersion(),
                                locations,
                                business.getCreatedAt(),
                                business.getUpdatedAt());
        }

        private BusinessLocationResponse toLocationResponse(
                        BusinessLocation location) {

                return new BusinessLocationResponse(
                                location.getId(),
                                location.getLatitude(),
                                location.getLongitude(),
                                location.getAddressLine1(),
                                location.getAddressLine2(),
                                location.getCity(),
                                location.getState(),
                                location.getCountry(),
                                location.getPostalCode(),
                                location.getStatus(),
                                location.getCreatedAt(),
                                location.getUpdatedAt());
        }

        // =========================================================
        // NORMALIZATION
        // =========================================================

        private String normalizeName(String value) {

                return value.trim();
        }

        private String normalizeDescription(String value) {

                return normalize(value);
        }

        private String normalize(String value) {

                if (value == null) {
                        return null;
                }

                String normalized = value.trim();

                return normalized.isEmpty()
                                ? null
                                : normalized;
        }

        @Observed(name = "business.location.deactivate", contextualName = "deactivate-business-location")
        @Transactional
        @Override
        public void deactivateLocation(UUID businessId, UUID locationId) {

                log.info(
                                "Deactivating business location. businessId={}, locationId={}",
                                businessId,
                                locationId);

                Business business = getBusiness(businessId);

                BusinessLocation location = findLocation(business, locationId);

                if (location.getStatus() == LocationStatus.INACTIVE) {
                        log.info(
                                        "Business location is already inactive. businessId={}, locationId={}",
                                        businessId,
                                        locationId);
                        return;
                }

                location.setStatus(LocationStatus.INACTIVE);

                BusinessLocationDeactivatedEvent event = new BusinessLocationDeactivatedEvent(
                                businessId,
                                locationId,
                                Instant.now());

                outboxService.save(
                                "BUSINESS",
                                businessId,
                                BusinessEventType.BUSINESS_LOCATION_DEACTIVATED.name(),
                                event);

                log.info(
                                "Business location deactivated and event created. businessId={}, locationId={}",
                                businessId,
                                locationId);
        }

}