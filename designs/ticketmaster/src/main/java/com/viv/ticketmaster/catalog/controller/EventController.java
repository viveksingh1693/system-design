package com.viv.ticketmaster.catalog.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.viv.ticketmaster.catalog.dto.CreateEventRequest;
import com.viv.ticketmaster.catalog.dto.EventResponse;
import com.viv.ticketmaster.catalog.service.CatalogService;
import com.viv.ticketmaster.inventory.dto.CreateShowRequest;
import com.viv.ticketmaster.inventory.dto.ShowResponse;
import com.viv.ticketmaster.inventory.service.InventoryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
@Tag(name = "Catalog", description = "Event discovery and onboarding APIs")
public class EventController {

    private final CatalogService catalogService;
    private final InventoryService inventoryService;


    @PostMapping
    @Operation(summary = "Create an event", description = "Creates a discoverable event in the catalog service.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Event created successfully",
                content = @Content(schema = @Schema(implementation = EventResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid event payload")
    })
    public EventResponse createEvent(@Valid @RequestBody CreateEventRequest request) {
        return EventResponse.from(catalogService.createEvent(request));
    }

    @GetMapping
    @Operation(summary = "Search events", description = "Filters events by city and free-text query.")
    public List<EventResponse> searchEvents(
            @RequestParam(required = false) String city,
            @RequestParam(name = "q", required = false) String query
    ) {
        return catalogService.searchEvents(city, query)
                .stream()
                .map(EventResponse::from)
                .toList();
    }

    @PostMapping("/{eventId}/shows")
    @Operation(summary = "Create a show", description = "Adds a dated show with seat inventory for an event.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Show created successfully",
                content = @Content(schema = @Schema(implementation = ShowResponse.class))),
        @ApiResponse(responseCode = "404", description = "Event not found"),
        @ApiResponse(responseCode = "400", description = "Invalid show payload")
    })
    public ShowResponse createShow(
            @PathVariable Long eventId,
            @Valid @RequestBody CreateShowRequest request
    ) {
        return ShowResponse.from(inventoryService.createShow(eventId, request));
    }
}
