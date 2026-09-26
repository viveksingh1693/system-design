package com.viv.location.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.viv.location.model.LocationUpdateRequest;
import com.viv.location.service.LocationService;

@RequiredArgsConstructor 
@RestController
@RequestMapping("/api/v1/locations")
public class LocationController {

    private final LocationService locationService;


    @PostMapping
    public ResponseEntity<Void> updateLocation(
            @Valid @RequestBody LocationUpdateRequest request) {

        locationService.updateLocation(request);

        return ResponseEntity.accepted().build();
    }
}