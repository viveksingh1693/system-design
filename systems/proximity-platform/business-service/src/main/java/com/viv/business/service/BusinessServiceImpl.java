package com.viv.business.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
import com.viv.business.exception.BusinessCategoryNotFoundException;
import com.viv.business.exception.BusinessNotFoundException;
import com.viv.business.repository.BusinessCategoryRepository;
import com.viv.business.repository.BusinessRepository;

import io.micrometer.observation.annotation.Observed;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class BusinessServiceImpl implements BusinessService {

    private final OutboxService outboxService;
    private final BusinessRepository businessRepository;
    private final BusinessCategoryRepository businessCategoryRepository;

    @Observed(
            name = "business.create",
            contextualName = "create-business")
    @Transactional
    @Override
    public BusinessResponse create(
            CreateBusinessRequest request) {

        log.info(
                "Creating business with request: {}",
                request
        );

        BusinessCategory category =
                businessCategoryRepository.findById(
                        request.categoryId()
                ).orElseThrow(
                        () -> new BusinessCategoryNotFoundException(
                                "Business category not found with ID: "
                                        + request.categoryId()
                        )
                );

        Business business =
                Business.builder()
                        .name(normalizeName(request.name()))
                        .description(
                                normalizeDescription(
                                        request.description()
                                )
                        )
                        .category(category)
                        .status(BusinessStatus.ACTIVE)
                        .locations(new ArrayList<>())
                        .build();

        for (CreateBusinessLocationRequest locationRequest
                : request.locations()) {

            BusinessLocation location =
                    createLocation(locationRequest);

            business.addLocation(location);
        }

        Business savedBusiness =
                businessRepository.save(business);

        log.info(
                "Created business with id: {} for categoryId: {}",
                savedBusiness.getId(),
                savedBusiness.getCategory().getId()
        );

        /*
         * ---------------------------------------------------------
         * BUSINESS_CREATED
         * ---------------------------------------------------------
         */

        BusinessCreatedEvent businessEvent =
                new BusinessCreatedEvent(
                        savedBusiness.getId(),
                        savedBusiness.getCategory().getId(),
                        savedBusiness.getName(),
                        savedBusiness.getStatus().name(),
                        Instant.now()
                );

        outboxService.save(
                "BUSINESS",
                savedBusiness.getId(),
                BusinessEventType.BUSINESS_CREATED.name(),
                businessEvent
        );

        /*
         * ---------------------------------------------------------
         * BUSINESS_LOCATION_CREATED
         * ---------------------------------------------------------
         *
         * One location event is created for every location.
         *
         * aggregateId = businessId
         *
         * This keeps Kafka partitioning/order based on businessId.
         *
         * locationId is carried inside the event payload.
         * ---------------------------------------------------------
         */

        for (BusinessLocation location
                : savedBusiness.getLocations()) {

            BusinessLocationCreatedEvent locationEvent =
                    new BusinessLocationCreatedEvent(
                            savedBusiness.getId(),
                            location.getId(),
                            savedBusiness.getCategory().getId(),
                            savedBusiness.getName(),
                            savedBusiness.getStatus().name(),
                            location.getLatitude(),
                            location.getLongitude(),
                            location.getStatus().name(),
                            Instant.now()
                    );

            outboxService.save(
                    "BUSINESS",
                    savedBusiness.getId(),
                    BusinessEventType.BUSINESS_LOCATION_CREATED.name(),
                    locationEvent
            );

            log.info(
                    "Created BUSINESS_LOCATION_CREATED event. " +
                    "businessId={}, locationId={}",
                    savedBusiness.getId(),
                    location.getId()
            );
        }

        return toResponse(savedBusiness);
    }

    @Observed(
            name = "business.get",
            contextualName = "get-business")
    @Override
    @Transactional(readOnly = true)
    public BusinessResponse getById(UUID id) {

        log.info(
                "Fetching business by id: {}",
                id
        );

        Business business =
                businessRepository
                        .findById(id)
                        .orElseThrow(
                                () -> new BusinessNotFoundException(
                                        "Business not found with ID: "
                                                + id
                                )
                        );

        return toResponse(business);
    }

    @Observed(
            name = "business.update",
            contextualName = "update-business")
    @Transactional
    @Override
    public BusinessResponse update(
            UUID id,
            UpdateBusinessRequest request) {

        log.info(
                "Updating business id: {} with request: {}",
                id,
                request
        );

        Business business =
                businessRepository
                        .findById(id)
                        .orElseThrow(
                                () -> new BusinessNotFoundException(
                                        "Business not found with ID: "
                                                + id
                                )
                        );

        business.setName(
                normalizeName(request.name())
        );

        business.setDescription(
                normalizeDescription(request.description())
        );

        if (request.locations() != null) {

            updateLocations(
                    business,
                    request.locations()
            );
        }

        log.info(
                "Updated business id: {}",
                id
        );

        return toResponse(business);
    }

    @Observed(
            name = "business.deactivate",
            contextualName = "deactivate-business")
    @Transactional
    @Override
    public void deactivate(UUID id) {

        log.info(
                "Deactivating business id: {}",
                id
        );

        Business business =
                getBusiness(id);

        business.setStatus(
                BusinessStatus.INACTIVE
        );

        log.info(
                "Business id: {} deactivated",
                id
        );
    }

    @Observed(
            name = "business.suspend",
            contextualName = "suspend-business")
    @Transactional
    @Override
    public void suspend(UUID id) {

        log.info(
                "Suspending business id: {}",
                id
        );

        Business business =
                getBusiness(id);

        business.setStatus(
                BusinessStatus.SUSPENDED
        );

        log.info(
                "Business id: {} suspended",
                id
        );
    }

    @Observed(
            name = "business.activate",
            contextualName = "activate-business")
    @Transactional
    @Override
    public void activate(UUID id) {

        log.info(
                "Activating business id: {}",
                id
        );

        Business business =
                getBusiness(id);

        business.setStatus(
                BusinessStatus.ACTIVE
        );

        log.info(
                "Business id: {} activated",
                id
        );
    }

    private Business getBusiness(UUID id) {

        return businessRepository
                .findById(id)
                .orElseThrow(
                        () -> new BusinessNotFoundException(
                                "Business not found with ID: "
                                        + id
                        )
                );
    }

    private BusinessLocation createLocation(
            CreateBusinessLocationRequest request) {

        return BusinessLocation.builder()
                .latitude(request.latitude())
                .longitude(request.longitude())
                .addressLine1(
                        normalize(
                                request.addressLine1()
                        )
                )
                .addressLine2(
                        normalize(
                                request.addressLine2()
                        )
                )
                .city(
                        normalize(
                                request.city()
                        )
                )
                .state(
                        normalize(
                                request.state()
                        )
                )
                .country(
                        normalize(
                                request.country()
                        )
                )
                .postalCode(
                        normalize(
                                request.postalCode()
                        )
                )
                .status(LocationStatus.ACTIVE)
                .build();
    }

    private void updateLocations(
            Business business,
            List<UpdateBusinessLocationRequest> requests) {

        for (UpdateBusinessLocationRequest request
                : requests) {

            if (request.id() == null) {

                BusinessLocation newLocation =
                        BusinessLocation.builder()
                                .latitude(request.latitude())
                                .longitude(request.longitude())
                                .addressLine1(
                                        normalize(
                                                request.addressLine1()
                                        )
                                )
                                .addressLine2(
                                        normalize(
                                                request.addressLine2()
                                        )
                                )
                                .city(
                                        normalize(
                                                request.city()
                                        )
                                )
                                .state(
                                        normalize(
                                                request.state()
                                        )
                                )
                                .country(
                                        normalize(
                                                request.country()
                                        )
                                )
                                .postalCode(
                                        normalize(
                                                request.postalCode()
                                        )
                                )
                                .status(LocationStatus.ACTIVE)
                                .build();

                business.addLocation(newLocation);

                continue;
            }

            BusinessLocation location =
                    findLocation(
                            business,
                            request.id()
                    );

            location.setLatitude(
                    request.latitude()
            );

            location.setLongitude(
                    request.longitude()
            );

            location.setAddressLine1(
                    normalize(
                            request.addressLine1()
                    )
            );

            location.setAddressLine2(
                    normalize(
                            request.addressLine2()
                    )
            );

            location.setCity(
                    normalize(
                            request.city()
                    )
            );

            location.setState(
                    normalize(
                            request.state()
                    )
            );

            location.setCountry(
                    normalize(
                            request.country()
                    )
            );

            location.setPostalCode(
                    normalize(
                            request.postalCode()
                    )
            );
        }
    }

    private BusinessLocation findLocation(
            Business business,
            UUID locationId) {

        return business.getLocations()
                .stream()
                .filter(
                        location ->
                                location.getId().equals(locationId)
                )
                .findFirst()
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                "Location does not belong to business: "
                                        + locationId
                        )
                );
    }

    private BusinessResponse toResponse(
            Business business) {

        List<BusinessLocationResponse> locations =
                business.getLocations()
                        .stream()
                        .map(this::toLocationResponse)
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
                business.getUpdatedAt()
        );
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
                location.getUpdatedAt()
        );
    }

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
}