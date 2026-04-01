package com.viv.ticketmaster.catalog.dto;

import com.viv.ticketmaster.catalog.entity.Event;

public record EventResponse(
        Long id,
        String title,
        String performer,
        String city,
        String venueName,
        String category
) {

    public static EventResponse from(Event event) {
        return new EventResponse(
                event.getId(),
                event.getTitle(),
                event.getPerformer(),
                event.getCity(),
                event.getVenueName(),
                event.getCategory()
        );
    }
}
