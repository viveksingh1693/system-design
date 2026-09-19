package com.viv.business.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.viv.business.dto.BusinessCategoryResponse;
import com.viv.business.dto.CreateBusinessCategoryRequest;
import com.viv.business.dto.UpdateBusinessCategoryRequest;
import com.viv.business.service.BusinessCategoryService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/business-categories")
@RequiredArgsConstructor
@Slf4j 
public class BusinessCategoryController {

    private final BusinessCategoryService categoryService;

    @PostMapping
    public ResponseEntity<BusinessCategoryResponse> create(
            @Valid @RequestBody CreateBusinessCategoryRequest request) {

        log.info("Creating business category with request: {}", request);

        BusinessCategoryResponse response = categoryService.create(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/{categoryId}")
    public ResponseEntity<BusinessCategoryResponse> getById(
            @PathVariable UUID categoryId) {

        log.info("Fetching business category with id: {}", categoryId);

        BusinessCategoryResponse response = categoryService.getById(categoryId);

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<BusinessCategoryResponse>> getActiveCategories() {

        log.info("Fetching active business categories");

        List<BusinessCategoryResponse> response = categoryService.getActiveCategories();

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{categoryId}")
    public ResponseEntity<BusinessCategoryResponse> update(
            @PathVariable UUID categoryId,
            @Valid @RequestBody UpdateBusinessCategoryRequest request) {

        log.info("Updating business category with id: {} and request: {}", categoryId, request);

        BusinessCategoryResponse response = categoryService.update(
                categoryId,
                request);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{categoryId}/deactivate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(
            @PathVariable UUID categoryId) {

        log.info("Deactivating business category with id: {}", categoryId);
        categoryService.deactivate(categoryId);
    }
}