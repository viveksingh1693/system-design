package com.viv.business.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.viv.business.dto.BusinessLocationResponse;
import com.viv.business.dto.CreateBusinessLocationRequest;
import com.viv.business.dto.UpdateBusinessLocationRequest;
import com.viv.business.service.BusinessLocationService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(
        "/api/v1/businesses/{businessId}/locations"
)
@RequiredArgsConstructor
@Slf4j 
public class BusinessLocationController {

    private final BusinessLocationService locationService;

    @PostMapping
    public ResponseEntity<BusinessLocationResponse> create(
            @PathVariable UUID businessId,
            @Valid @RequestBody
            CreateBusinessLocationRequest request) {

        log.info("Creating business location for businessId: {} with request: {}", businessId, request);

        BusinessLocationResponse response = locationService.create(
                businessId,
                request
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<BusinessLocationResponse>>
    getLocations(
            @PathVariable UUID businessId) {

        log.info("Fetching business locations for businessId: {}", businessId);

        List<BusinessLocationResponse> response = locationService.getByBusinessId(
                businessId
        );

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{locationId}")
    public ResponseEntity<BusinessLocationResponse> update(
            @PathVariable UUID businessId,
            @PathVariable UUID locationId,
            @Valid @RequestBody
            UpdateBusinessLocationRequest request) {

        log.info("Updating business location for businessId: {} and locationId: {} with request: {}",
                businessId, locationId, request);

        BusinessLocationResponse response = locationService.update(
                businessId,
                locationId,
                request
        );

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{locationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(
            @PathVariable UUID businessId,
            @PathVariable UUID locationId) {

        log.info("Deactivating business location for businessId: {} and locationId: {}",
                businessId, locationId);

        locationService.deactivate(
                businessId,
                locationId
        );
    }
}