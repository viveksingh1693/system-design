package com.viv.business_service.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.viv.business.dto.BusinessCategoryResponse;
import com.viv.business.dto.BusinessResponse;
import com.viv.business.dto.CreateBusinessCategoryRequest;
import com.viv.business.dto.CreateBusinessLocationRequest;
import com.viv.business.dto.CreateBusinessRequest;
import com.viv.business.repository.BusinessRepository;
import com.viv.business.service.BusinessCategoryService;
import com.viv.business.service.BusinessService;
import com.viv.business_service.AbstractIntegrationTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BusinessServiceIntegrationTest
        extends AbstractIntegrationTest {

    @Autowired
    private BusinessService businessService;

    @Autowired
    private BusinessCategoryService categoryService;

    @Autowired
    private BusinessRepository businessRepository;

    @Test
    void shouldCreateBusinessWithLocation() {

        BusinessCategoryResponse category =
                categoryService.create(
                        new CreateBusinessCategoryRequest(
                                "Restaurant",
                                "RESTAURANT",
                                "Restaurants"
                        )
                );

        CreateBusinessLocationRequest location =
                new CreateBusinessLocationRequest(
                        28.4595,
                        77.0266,
                        "MG Road",
                        null,
                        "Gurugram",
                        "Haryana",
                        "India",
                        "122001"
                );

        CreateBusinessRequest request =
                new CreateBusinessRequest(
                        "Test Restaurant",
                        "Test restaurant",
                        category.id(),
                        List.of(location)
                );

        BusinessResponse response =
                businessService.create(request);

        assertThat(response.id()).isNotNull();

        assertThat(response.name())
                .isEqualTo("Test Restaurant");

        assertThat(response.categoryId())
                .isEqualTo(category.id());

        assertThat(response.locations())
                .hasSize(1);

        assertThat(response.locations().get(0).latitude())
                .isEqualTo(28.4595);

        assertThat(response.locations().get(0).longitude())
                .isEqualTo(77.0266);

        assertThat(businessRepository.count())
                .isEqualTo(1);
    }
}