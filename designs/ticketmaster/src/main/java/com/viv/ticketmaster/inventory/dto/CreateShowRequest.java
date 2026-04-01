package com.viv.ticketmaster.inventory.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateShowRequest(
        @NotNull(message = "must not be null")
        @Future(message = "must be in the future")
        LocalDateTime startTime,
        @NotNull(message = "must not be null")
        @DecimalMin(value = "0.0", inclusive = false, message = "must be greater than 0")
        BigDecimal price,
        @Min(value = 1, message = "must be at least 1")
        int totalSeats
) {
}
