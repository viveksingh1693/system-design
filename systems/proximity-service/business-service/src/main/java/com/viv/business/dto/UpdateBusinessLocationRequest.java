package com.viv.business.dto;

import jakarta.validation.constraints.*;

import java.util.UUID;

public record UpdateBusinessLocationRequest(

        UUID id,

        @NotNull @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0") Double latitude,

        @NotNull @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0") Double longitude,

        @Size(max = 300) String addressLine1,

        @Size(max = 300) String addressLine2,

        @Size(max = 100) String city,

        @Size(max = 100) String state,

        @Size(max = 100) String country,

        @Size(max = 20) String postalCode) {
}