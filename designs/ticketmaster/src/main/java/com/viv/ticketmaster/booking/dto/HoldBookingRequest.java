package com.viv.ticketmaster.booking.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record HoldBookingRequest(
        @NotNull(message = "must not be null")
        Long showId,
        @NotBlank(message = "must not be blank")
        @Email(message = "must be a valid email address")
        String customerEmail,
        @Min(value = 1, message = "must be at least 1")
        int seatCount
) {
}
