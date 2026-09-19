package com.viv.business.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.viv.business.dto.BusinessResponse;
import com.viv.business.dto.CreateBusinessRequest;
import com.viv.business.dto.UpdateBusinessRequest;
import com.viv.business.service.BusinessService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/businesses")
@RequiredArgsConstructor
public class BusinessController {

    private final BusinessService businessService;

    @PostMapping
    public ResponseEntity<BusinessResponse> create(
            @Valid @RequestBody CreateBusinessRequest request) {

        BusinessResponse response = businessService.create(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/{businessId}")
    public ResponseEntity<BusinessResponse> getById(
            @PathVariable UUID businessId) {

        return ResponseEntity.ok(
                businessService.getById(businessId));
    }

    @PatchMapping("/{businessId}")
    public ResponseEntity<BusinessResponse> update(
            @PathVariable UUID businessId,
            @Valid @RequestBody UpdateBusinessRequest request) {

        return ResponseEntity.ok(
                businessService.update(
                        businessId,
                        request));
    }

    @PostMapping("/{businessId}/activate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void activate(
            @PathVariable UUID businessId) {

        businessService.activate(businessId);
    }

    @PostMapping("/{businessId}/suspend")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void suspend(
            @PathVariable UUID businessId) {

        businessService.suspend(businessId);
    }

    @PostMapping("/{businessId}/deactivate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(
            @PathVariable UUID businessId) {

        businessService.deactivate(businessId);
    }
}