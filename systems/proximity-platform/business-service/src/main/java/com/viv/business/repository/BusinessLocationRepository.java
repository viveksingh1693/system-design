package com.viv.business.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.viv.business.entity.BusinessLocation;
import com.viv.business.enums.LocationStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BusinessLocationRepository
        extends JpaRepository<BusinessLocation, UUID> {

    List<BusinessLocation> findByBusiness_Id(
            UUID businessId
    );

    List<BusinessLocation> findByBusiness_IdAndStatus(
            UUID businessId,
            LocationStatus status
    );

    List<BusinessLocation> findByStatus(
            LocationStatus status
    );

    Optional<BusinessLocation> findByIdAndBusiness_Id(
            UUID locationId,
            UUID businessId
    );
}