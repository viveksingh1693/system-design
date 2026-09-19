package com.viv.business.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.viv.business.dto.BusinessResponse;
import com.viv.business.dto.CreateBusinessRequest;
import com.viv.business.dto.UpdateBusinessRequest;
import com.viv.business.service.BusinessService;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/businesses")
@RequiredArgsConstructor
public class BusinessController {

    private final BusinessService businessService;

    @PostMapping
    public ResponseEntity<BusinessResponse> create(
            @Valid @RequestBody CreateBusinessRequest request) {

        log.info("Creating business with request: {}", request);

        BusinessResponse response = businessService.create(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/{businessId}")
    public ResponseEntity<BusinessResponse> getById(
            @PathVariable UUID businessId) {

        log.info("Fetching business with id: {}", businessId);

        BusinessResponse response = businessService.getById(businessId);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{businessId}")
    public ResponseEntity<BusinessResponse> update(
            @PathVariable UUID businessId,
            @Valid @RequestBody UpdateBusinessRequest request) {

        log.info("Updating business with id: {} and payload: {}", businessId, request);

        BusinessResponse response = businessService.update(
                businessId,
                request);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{businessId}/activate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void activate(
            @PathVariable UUID businessId) {

        log.info("Activating business with id: {}", businessId);
        businessService.activate(businessId);
    }

    @PostMapping("/{businessId}/suspend")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void suspend(
            @PathVariable UUID businessId) {

        log.info("Suspending business with id: {}", businessId);
        businessService.suspend(businessId);
    }

    @PostMapping("/{businessId}/deactivate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(
            @PathVariable UUID businessId) {

        log.info("Deactivating business with id: {}", businessId);
        businessService.deactivate(businessId);
    }
}