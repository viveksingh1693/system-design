package com.viv.business_service.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.viv.business.dto.BusinessCategoryResponse;
import com.viv.business.dto.CreateBusinessCategoryRequest;
import com.viv.business.repository.BusinessCategoryRepository;
import com.viv.business.service.BusinessCategoryService;
import com.viv.business_service.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

class BusinessCategoryServiceIntegrationTest
        extends AbstractIntegrationTest {

    @Autowired
    private BusinessCategoryService categoryService;

    @Autowired
    private BusinessCategoryRepository repository;

    @Test
    void shouldCreateBusinessCategory() {

        CreateBusinessCategoryRequest request = new CreateBusinessCategoryRequest(
                "Restaurant",
                "RESTAURANT",
                "Restaurants and food outlets");

        BusinessCategoryResponse response = categoryService.create(request);

        assertThat(response.id()).isNotNull();
        assertThat(response.name())
                .isEqualTo("Restaurant");
        assertThat(response.code())
                .isEqualTo("RESTAURANT");
        assertThat(response.status().name())
                .isEqualTo("ACTIVE");

        assertThat(repository.count())
                .isEqualTo(1);
    }
}