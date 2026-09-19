package com.viv.business.dto;


import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.viv.business.enums.BusinessStatus;

public record BusinessResponse(

        UUID id,

        String name,

        String description,

        UUID categoryId,

        BusinessStatus status,

        Long version,

        List<BusinessLocationResponse> locations,

        Instant createdAt,

        Instant updatedAt
) {
}