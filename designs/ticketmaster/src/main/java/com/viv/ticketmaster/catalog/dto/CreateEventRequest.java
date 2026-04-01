package com.viv.ticketmaster.catalog.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateEventRequest(
        @NotBlank(message = "must not be blank")
        String title,
        @NotBlank(message = "must not be blank")
        String performer,
        @NotBlank(message = "must not be blank")
        String city,
        @NotBlank(message = "must not be blank")
        String venueName,
        @NotBlank(message = "must not be blank")
        String category
) {
}
