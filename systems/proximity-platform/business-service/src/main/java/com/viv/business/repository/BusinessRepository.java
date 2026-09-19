package com.viv.business.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.viv.business.entity.Business;
import com.viv.business.enums.BusinessStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BusinessRepository
        extends JpaRepository<Business, UUID> {

    Optional<Business> findByIdAndStatus(
            UUID id,
            BusinessStatus status);

    List<Business> findByCategory_Id(
            UUID categoryId);

    List<Business> findByStatus(
            BusinessStatus status);

    List<Business> findByCategory_IdAndStatus(
            UUID categoryId,
            BusinessStatus status);
}