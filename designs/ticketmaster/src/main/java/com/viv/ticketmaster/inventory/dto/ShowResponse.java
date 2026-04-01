package com.viv.ticketmaster.inventory.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.viv.ticketmaster.inventory.entity.Show;

public record ShowResponse(
        Long id,
        Long eventId,
        String eventTitle,
        String performer,
        String city,
        String venueName,
        LocalDateTime startTime,
        BigDecimal price,
        int totalSeats,
        int availableSeats
) {

    public static ShowResponse from(Show show) {
        return new ShowResponse(
                show.getId(),
                show.getEvent().getId(),
                show.getEvent().getTitle(),
                show.getEvent().getPerformer(),
                show.getEvent().getCity(),
                show.getEvent().getVenueName(),
                show.getStartTime(),
                show.getPrice(),
                show.getTotalSeats(),
                show.getAvailableSeats()
        );
    }
}
