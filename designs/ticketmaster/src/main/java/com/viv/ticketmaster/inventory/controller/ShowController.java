package com.viv.ticketmaster.inventory.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.viv.ticketmaster.inventory.dto.ShowResponse;
import com.viv.ticketmaster.inventory.service.InventoryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/shows")
@RequiredArgsConstructor
@Tag(name = "Inventory", description = "Show inventory and seat availability APIs")
public class ShowController {

    private final InventoryService inventoryService;


    @GetMapping
    @Operation(summary = "Search shows", description = "Filters shows by city, artist or title query, and time window.")
    public List<ShowResponse> searchShows(
            @RequestParam(required = false) String city,
            @RequestParam(name = "q", required = false) String query,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        return inventoryService.searchShows(city, query, from, to)
                .stream()
                .map(ShowResponse::from)
                .toList();
    }

    @GetMapping("/{showId}")
    @Operation(summary = "Get show details", description = "Returns show metadata together with live seat availability.")
    public ShowResponse getShow(@PathVariable Long showId) {
        return ShowResponse.from(inventoryService.getShow(showId));
    }
}
