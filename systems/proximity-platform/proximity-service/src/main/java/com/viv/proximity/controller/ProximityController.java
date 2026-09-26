package com.viv.proximity.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.viv.proximity.dto.NearbyBusinessResponse;
import com.viv.proximity.service.ProximityQueryService;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/proximity")
@RequiredArgsConstructor
@Validated
@Slf4j
public class ProximityController {

    private final ProximityQueryService proximityQueryService;

    @GetMapping("/businesses")
    public ResponseEntity<List<NearbyBusinessResponse>> findNearby(

            @RequestParam @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0") double latitude,

            @RequestParam @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0") double longitude,

            @RequestParam(defaultValue = "5000") @Positive @Max(50000) double radius,

            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {

        log.info("Finding nearby businesses: latitude={}, longitude={}, radius={}, limit={}", latitude, longitude, radius, limit);
        List<NearbyBusinessResponse> businesses = proximityQueryService.findNearby(
                latitude,
                longitude,
                radius,
                limit);

        return ResponseEntity.ok(businesses);
    }
}