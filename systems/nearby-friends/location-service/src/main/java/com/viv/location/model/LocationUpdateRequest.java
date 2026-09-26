package com.viv.location.model;

import java.time.Instant;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LocationUpdateRequest(

    @NotBlank 
    String userId,

    @DecimalMin (value = "-90.0", message = "Latitude must be greater than or equal to -180.0")
    @DecimalMax (value = "90.0", message = "Latitude must be less than or equal to 180.0")
    Double latitude,

    @NotNull 
    @DecimalMin (value = "-180.0", message = "Longitude must be greater than or equal to -180.0")
    @DecimalMax (value = "180.0", message = "Longitude must be less than or equal to 180.0")
    Double longitude,

    @NotNull 
    @DecimalMin (value = "0.0", message = "Accuracy must be a positive value")
    Double accuracyMeters,

    @NotNull 
    Instant timestamp
) {

}
