package com.viv.business.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.viv.business.dto.BusinessCategoryResponse;
import com.viv.business.dto.CreateBusinessCategoryRequest;
import com.viv.business.dto.UpdateBusinessCategoryRequest;
import com.viv.business.entity.BusinessCategory;
import com.viv.business.enums.CategoryStatus;
import com.viv.business.exception.BusinessCategoryAlreadyExistsException;
import com.viv.business.exception.BusinessCategoryNotFoundException;
import com.viv.business.repository.BusinessCategoryRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class BusinessCategoryServiceImpl
        implements BusinessCategoryService {

    private final BusinessCategoryRepository repository;

    @Override
    public BusinessCategoryResponse create(
            CreateBusinessCategoryRequest request) {

        String name = normalizeName(request.name());
        String code = normalizeCode(request.code());

        validateDuplicateCode(code);
        validateDuplicateName(name);

        BusinessCategory category = BusinessCategory.builder()
                .name(name)
                .code(code)
                .description(normalizeDescription(request.description()))
                .status(CategoryStatus.ACTIVE)
                .build();

        BusinessCategory saved = repository.save(category);

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public BusinessCategoryResponse getById(UUID id) {

        BusinessCategory category = repository.findById(id)
                .orElseThrow(() -> new BusinessCategoryNotFoundException("Business category not found with ID: " + id));

        return toResponse(category);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BusinessCategoryResponse> getActiveCategories() {

        return repository
                .findByStatus(CategoryStatus.ACTIVE)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public BusinessCategoryResponse update(
            UUID id,
            UpdateBusinessCategoryRequest request) {

        BusinessCategory category = repository.findById(id)
                .orElseThrow(() -> new BusinessCategoryNotFoundException("Business category not found with ID: " + id));

        String name = normalizeName(request.name());

        if (!category.getName().equals(name)
                && repository.existsByName(name)) {

            throw new BusinessCategoryAlreadyExistsException(
                    "Business category name already exists: " + name);
        }

        category.setName(name);
        category.setDescription(
                normalizeDescription(request.description()));

        return toResponse(category);
    }

    @Override
    public void deactivate(UUID id) {

        BusinessCategory category = repository.findById(id)
                .orElseThrow(() -> new BusinessCategoryNotFoundException("Business category not found with ID: " + id));

        category.setStatus(CategoryStatus.INACTIVE);
    }

    private void validateDuplicateCode(String code) {

        if (repository.existsByCode(code)) {
            throw new BusinessCategoryAlreadyExistsException(
                    "Business category code already exists: " + code);
        }
    }

    private void validateDuplicateName(String name) {

        if (repository.existsByName(name)) {
            throw new BusinessCategoryAlreadyExistsException(
                    "Business category name already exists: " + name);

        }
    }

    private String normalizeName(String value) {
        return value.trim();
    }

    private String normalizeCode(String value) {
        return value
                .trim()
                .toUpperCase();
    }

    private String normalizeDescription(String value) {

        if (value == null) {
            return null;
        }

        String normalized = value.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }

    private BusinessCategoryResponse toResponse(
            BusinessCategory category) {

        return new BusinessCategoryResponse(
                category.getId(),
                category.getName(),
                category.getCode(),
                category.getDescription(),
                category.getStatus(),
                category.getCreatedAt(),
                category.getUpdatedAt());
    }
}