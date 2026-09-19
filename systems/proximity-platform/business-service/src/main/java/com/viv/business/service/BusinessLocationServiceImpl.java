package com.viv.business.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.viv.business.dto.BusinessLocationResponse;
import com.viv.business.dto.CreateBusinessLocationRequest;
import com.viv.business.dto.UpdateBusinessLocationRequest;
import com.viv.business.entity.Business;
import com.viv.business.entity.BusinessLocation;
import com.viv.business.enums.LocationStatus;
import com.viv.business.exception.BusinessLocationNotFoundException;
import com.viv.business.exception.BusinessNotFoundException;
import com.viv.business.repository.BusinessLocationRepository;
import com.viv.business.repository.BusinessRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j 
public class BusinessLocationServiceImpl
        implements BusinessLocationService {

    private final BusinessRepository businessRepository;

    private final BusinessLocationRepository locationRepository;

    @Override
    public BusinessLocationResponse create(
            UUID businessId,
            CreateBusinessLocationRequest request) {

        log.info("Creating business location for businessId: {} with request: {}", businessId, request);

        Business business = getBusiness(businessId);

        BusinessLocation location = BusinessLocation.builder()
                .business(business)
                .latitude(request.latitude())
                .longitude(request.longitude())
                .addressLine1(
                        normalize(request.addressLine1()))
                .addressLine2(
                        normalize(request.addressLine2()))
                .city(
                        normalize(request.city()))
                .state(
                        normalize(request.state()))
                .country(
                        normalize(request.country()))
                .postalCode(
                        normalize(request.postalCode()))
                .status(LocationStatus.ACTIVE)
                .build();

        BusinessLocation saved = locationRepository.save(location);

        log.info("Created business location: {} for businessId: {}", saved.getId(), businessId);

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BusinessLocationResponse> getByBusinessId(
            UUID businessId) {

        log.info("Fetching active business locations for businessId: {}", businessId);

        // Verify business exists.
        getBusiness(businessId);

        List<BusinessLocationResponse> result = locationRepository
                .findByBusiness_IdAndStatus(
                        businessId,
                        LocationStatus.ACTIVE)
                .stream()
                .map(this::toResponse)
                .toList();

        log.info("Found {} active business locations for businessId: {}", result.size(), businessId);

        return result;
    }

    @Override
    public BusinessLocationResponse update(
            UUID businessId,
            UUID locationId,
            UpdateBusinessLocationRequest request) {

        log.info("Updating business location for businessId: {} and locationId: {} with request: {}",
                businessId, locationId, request);

        BusinessLocation location = getLocation(businessId, locationId);

        location.setLatitude(
                request.latitude());

        location.setLongitude(
                request.longitude());

        location.setAddressLine1(
                normalize(request.addressLine1()));

        location.setAddressLine2(
                normalize(request.addressLine2()));

        location.setCity(
                normalize(request.city()));

        location.setState(
                normalize(request.state()));

        location.setCountry(
                normalize(request.country()));

        location.setPostalCode(
                normalize(request.postalCode()));

        log.info("Updated business location: {} for businessId: {}", locationId, businessId);

        return toResponse(location);
    }

    @Override
    public void deactivate(
            UUID businessId,
            UUID locationId) {

        log.info("Deactivating business location: {} for businessId: {}", locationId, businessId);

        BusinessLocation location = getLocation(
                businessId,
                locationId);

        location.setStatus(
                LocationStatus.INACTIVE);

        log.info("Deactivated business location: {} for businessId: {}", locationId, businessId);
    }

    private Business getBusiness(UUID businessId) {

        return businessRepository
                .findById(businessId)
                .orElseThrow(() -> new BusinessNotFoundException(
                        "Business not found with businessId: " + businessId));

    }

    private BusinessLocation getLocation(
            UUID businessId,
            UUID locationId) {

        return locationRepository
                .findByIdAndBusiness_Id(
                        locationId,
                        businessId)
                .orElseThrow(() -> new BusinessLocationNotFoundException(
                        "Business location not found with locationId: " + locationId));
    }

    private BusinessLocationResponse toResponse(
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