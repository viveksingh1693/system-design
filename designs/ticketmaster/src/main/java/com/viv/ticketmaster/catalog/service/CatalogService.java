package com.viv.ticketmaster.catalog.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.viv.ticketmaster.catalog.dto.CreateEventRequest;
import com.viv.ticketmaster.catalog.entity.Event;
import com.viv.ticketmaster.catalog.repository.EventRepository;
import com.viv.ticketmaster.common.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CatalogService {

    private final EventRepository eventRepository;


    @Transactional
    public Event createEvent(CreateEventRequest request) {
        Event event = new Event();
        event.setTitle(request.title());
        event.setPerformer(request.performer());
        event.setCity(request.city());
        event.setVenueName(request.venueName());
        event.setCategory(request.category());
        return eventRepository.save(event);
    }

    @Transactional(readOnly = true)
    public List<Event> searchEvents(String city, String query) {
        return eventRepository.search(normalize(city), normalize(query));
    }

    @Transactional(readOnly = true)
    public Event getEvent(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event " + eventId + " was not found"));
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
