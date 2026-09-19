package com.viv.business.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
public class BusinessLocationController {

    private final BusinessLocationService locationService;

    @PostMapping
    public ResponseEntity<BusinessLocationResponse> create(
            @PathVariable UUID businessId,
            @Valid @RequestBody
            CreateBusinessLocationRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        locationService.create(
                                businessId,
                                request
                        )
                );
    }

    @GetMapping
    public ResponseEntity<List<BusinessLocationResponse>>
    getLocations(
            @PathVariable UUID businessId) {

        return ResponseEntity.ok(
                locationService.getByBusinessId(
                        businessId
                )
        );
    }

    @PatchMapping("/{locationId}")
    public ResponseEntity<BusinessLocationResponse> update(
            @PathVariable UUID businessId,
            @PathVariable UUID locationId,
            @Valid @RequestBody
            UpdateBusinessLocationRequest request) {

        return ResponseEntity.ok(
                locationService.update(
                        businessId,
                        locationId,
                        request
                )
        );
    }

    @DeleteMapping("/{locationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(
            @PathVariable UUID businessId,
            @PathVariable UUID locationId) {

        locationService.deactivate(
                businessId,
                locationId
        );
    }
}