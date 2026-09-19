package com.viv.business.dto;

import java.time.Instant;
import java.util.UUID;

import com.viv.business.enums.CategoryStatus;

public record BusinessCategoryResponse(

        UUID id,

        String name,

        String code,

        String description,

        CategoryStatus status,

        Instant createdAt,

        Instant updatedAt) {
}