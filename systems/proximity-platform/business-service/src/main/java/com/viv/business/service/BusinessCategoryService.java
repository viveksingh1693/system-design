package com.viv.business.service;

import java.util.List;
import java.util.UUID;

import com.viv.business.dto.BusinessCategoryResponse;
import com.viv.business.dto.CreateBusinessCategoryRequest;
import com.viv.business.dto.UpdateBusinessCategoryRequest;

public interface BusinessCategoryService {

    BusinessCategoryResponse create(
            CreateBusinessCategoryRequest request);

    BusinessCategoryResponse getById(
            UUID id);

    List<BusinessCategoryResponse> getActiveCategories();

    BusinessCategoryResponse update(
            UUID id,
            UpdateBusinessCategoryRequest request);

    void deactivate(
            UUID id);
}