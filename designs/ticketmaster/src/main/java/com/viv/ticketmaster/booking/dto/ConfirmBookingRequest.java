package com.viv.ticketmaster.booking.dto;

import jakarta.validation.constraints.NotBlank;

public record ConfirmBookingRequest(
        @NotBlank(message = "must not be blank")
        String paymentToken
) {
}
