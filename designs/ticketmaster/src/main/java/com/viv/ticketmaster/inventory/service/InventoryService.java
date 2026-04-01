package com.viv.ticketmaster.inventory.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.viv.ticketmaster.catalog.entity.Event;
import com.viv.ticketmaster.catalog.service.CatalogService;
import com.viv.ticketmaster.common.exception.InsufficientSeatsException;
import com.viv.ticketmaster.common.exception.ResourceNotFoundException;
import com.viv.ticketmaster.inventory.dto.CreateShowRequest;
import com.viv.ticketmaster.inventory.entity.Show;
import com.viv.ticketmaster.inventory.repository.ShowRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final ShowRepository showRepository;
    private final CatalogService catalogService;


    @Transactional
    public Show createShow(Long eventId, CreateShowRequest request) {
        Event event = catalogService.getEvent(eventId);

        Show show = new Show();
        show.setEvent(event);
        show.setStartTime(request.startTime());
        show.setPrice(request.price());
        show.setTotalSeats(request.totalSeats());
        show.setAvailableSeats(request.totalSeats());
        return showRepository.save(show);
    }

    @Transactional(readOnly = true)
    public List<Show> searchShows(String city, String query, LocalDateTime fromTime, LocalDateTime toTime) {
        return showRepository.search(normalize(city), normalize(query), fromTime, toTime);
    }

    @Transactional(readOnly = true)
    public Show getShow(Long showId) {
        return showRepository.findDetailedById(showId)
                .orElseThrow(() -> new ResourceNotFoundException("Show " + showId + " was not found"));
    }

    @Transactional
    public Show reserveSeats(Long showId, int seatCount) {
        Show show = showRepository.findByIdForUpdate(showId)
                .orElseThrow(() -> new ResourceNotFoundException("Show " + showId + " was not found"));

        if (show.getAvailableSeats() < seatCount) {
            throw new InsufficientSeatsException(
                    "Only " + show.getAvailableSeats() + " seats are available for show " + showId
            );
        }

        show.setAvailableSeats(show.getAvailableSeats() - seatCount);
        return show;
    }

    @Transactional
    public Show releaseSeats(Long showId, int seatCount) {
        Show show = showRepository.findByIdForUpdate(showId)
                .orElseThrow(() -> new ResourceNotFoundException("Show " + showId + " was not found"));

        int updatedAvailability = Math.min(show.getTotalSeats(), show.getAvailableSeats() + seatCount);
        show.setAvailableSeats(updatedAvailability);
        return show;
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
