package com.viv.business.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.viv.business.entity.BusinessCategory;
import com.viv.business.enums.CategoryStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BusinessCategoryRepository
        extends JpaRepository<BusinessCategory, UUID> {

    Optional<BusinessCategory> findByCode(String code);

    Optional<BusinessCategory> findByName(String name);

    Optional<BusinessCategory> findByCodeAndStatus(
            String code,
            CategoryStatus status);

    List<BusinessCategory> findByStatus(
            CategoryStatus status);

    boolean existsByCode(String code);

    boolean existsByName(String name);
}