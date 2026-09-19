package com.viv.business.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
public class BusinessCategoryController {

    private final BusinessCategoryService categoryService;

    @PostMapping
    public ResponseEntity<BusinessCategoryResponse> create(
            @Valid @RequestBody CreateBusinessCategoryRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(categoryService.create(request));
    }

    @GetMapping("/{categoryId}")
    public ResponseEntity<BusinessCategoryResponse> getById(
            @PathVariable UUID categoryId) {

        return ResponseEntity.ok(
                categoryService.getById(categoryId));
    }

    @GetMapping
    public ResponseEntity<List<BusinessCategoryResponse>> getActiveCategories() {

        return ResponseEntity.ok(
                categoryService.getActiveCategories());
    }

    @PatchMapping("/{categoryId}")
    public ResponseEntity<BusinessCategoryResponse> update(
            @PathVariable UUID categoryId,
            @Valid @RequestBody UpdateBusinessCategoryRequest request) {

        return ResponseEntity.ok(
                categoryService.update(
                        categoryId,
                        request));
    }

    @PostMapping("/{categoryId}/deactivate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(
            @PathVariable UUID categoryId) {

        categoryService.deactivate(categoryId);
    }
}