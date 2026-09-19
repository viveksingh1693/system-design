package com.viv.business.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.viv.business.dto.BusinessResponse;
import com.viv.business.dto.CreateBusinessLocationRequest;
import com.viv.business.dto.CreateBusinessRequest;
import com.viv.business.dto.UpdateBusinessLocationRequest;
import com.viv.business.dto.UpdateBusinessRequest;
import com.viv.business.entity.Business;
import com.viv.business.entity.BusinessCategory;
import com.viv.business.entity.BusinessLocation;
import com.viv.business.enums.BusinessStatus;
import com.viv.business.enums.LocationStatus;
import com.viv.business.exception.BusinessCategoryNotFoundException;
import com.viv.business.exception.BusinessNotFoundException;
import com.viv.business.repository.BusinessCategoryRepository;
import com.viv.business.repository.BusinessRepository;

@ExtendWith(MockitoExtension.class)
class BusinessServiceImplTest {

    @Mock
    private OutboxService outboxService;

    @Mock
    private BusinessRepository businessRepository;

    @Mock
    private BusinessCategoryRepository businessCategoryRepository;

    @InjectMocks
    private BusinessServiceImpl businessService;

    private BusinessCategory category;

    @BeforeEach
    void setUp() {
        category = BusinessCategory.builder()
                .id(UUID.randomUUID())
                .name("Restaurant")
                .code("RESTAURANT")
                .description("Food services")
                .status(com.viv.business.enums.CategoryStatus.ACTIVE)
                .build();
    }

    @Test
    void shouldCreateBusinessAndPersistOutboxEvent() {
        CreateBusinessLocationRequest locationRequest = new CreateBusinessLocationRequest(
                28.4595,
                77.0266,
                "MG Road",
                null,
                "Gurugram",
                "Haryana",
                "India",
                "122001");

        CreateBusinessRequest request = new CreateBusinessRequest(
                "  Test Restaurant  ",
                "  Fresh food and coffee  ",
                category.getId(),
                List.of(locationRequest));

        when(businessCategoryRepository.findById(category.getId()))
                .thenReturn(java.util.Optional.of(category));

        Business savedBusiness = Business.builder()
                .id(UUID.randomUUID())
                .name("Test Restaurant")
                .description("Fresh food and coffee")
                .category(category)
                .status(BusinessStatus.ACTIVE)
                .locations(new ArrayList<>())
                .build();

        BusinessLocation savedLocation = BusinessLocation.builder()
                .id(UUID.randomUUID())
                .business(savedBusiness)
                .latitude(28.4595)
                .longitude(77.0266)
                .addressLine1("MG Road")
                .city("Gurugram")
                .state("Haryana")
                .country("India")
                .postalCode("122001")
                .status(LocationStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        savedBusiness.addLocation(savedLocation);
        when(businessRepository.save(any(Business.class))).thenReturn(savedBusiness);

        BusinessResponse response = businessService.create(request);

        assertThat(response.id()).isEqualTo(savedBusiness.getId());
        assertThat(response.name()).isEqualTo("Test Restaurant");
        assertThat(response.description()).isEqualTo("Fresh food and coffee");
        assertThat(response.categoryId()).isEqualTo(category.getId());
        assertThat(response.status()).isEqualTo(BusinessStatus.ACTIVE);
        assertThat(response.locations()).hasSize(1);
        assertThat(response.locations().get(0).city()).isEqualTo("Gurugram");

        verify(businessRepository).save(any(Business.class));
        verify(outboxService).save(eq("BUSINESS"), eq(savedBusiness.getId()), eq("BUSINESS_CREATED"), any());
    }

    @Test
    void shouldCreateBusinessWithMultipleLocations() {
        CreateBusinessLocationRequest firstLocation = new CreateBusinessLocationRequest(
                12.9716,
                77.5946,
                "Brigade Road",
                "Suite 15",
                "Bengaluru",
                "Karnataka",
                "India",
                "560001");

        CreateBusinessLocationRequest secondLocation = new CreateBusinessLocationRequest(
                13.0068,
                77.5545,
                "MG Road",
                null,
                "Bengaluru",
                "Karnataka",
                "India",
                "560001");

        CreateBusinessRequest request = new CreateBusinessRequest(
                "Bengaluru Cafe",
                "City cafe",
                category.getId(),
                List.of(firstLocation, secondLocation));

        when(businessCategoryRepository.findById(category.getId()))
                .thenReturn(java.util.Optional.of(category));

        Business savedBusiness = Business.builder()
                .id(UUID.randomUUID())
                .name("Bengaluru Cafe")
                .description("City cafe")
                .category(category)
                .status(BusinessStatus.ACTIVE)
                .locations(new ArrayList<>())
                .build();

        savedBusiness.addLocation(BusinessLocation.builder()
                .id(UUID.randomUUID())
                .business(savedBusiness)
                .latitude(12.9716)
                .longitude(77.5946)
                .addressLine1("Brigade Road")
                .addressLine2("Suite 15")
                .city("Bengaluru")
                .state("Karnataka")
                .country("India")
                .postalCode("560001")
                .status(LocationStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build());

        savedBusiness.addLocation(BusinessLocation.builder()
                .id(UUID.randomUUID())
                .business(savedBusiness)
                .latitude(13.0068)
                .longitude(77.5545)
                .addressLine1("MG Road")
                .city("Bengaluru")
                .state("Karnataka")
                .country("India")
                .postalCode("560001")
                .status(LocationStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build());

        when(businessRepository.save(any(Business.class))).thenReturn(savedBusiness);

        BusinessResponse response = businessService.create(request);

        assertThat(response.locations()).hasSize(2);
        assertThat(response.locations()).extracting("city").containsExactlyInAnyOrder("Bengaluru", "Bengaluru");
        assertThat(response.locations()).extracting("addressLine1").contains("Brigade Road", "MG Road");
    }

    @Test
    void shouldThrowWhenCategoryDoesNotExist() {
        UUID missingCategoryId = UUID.randomUUID();
        CreateBusinessRequest request = new CreateBusinessRequest(
                "Missing Category Business",
                "No category",
                missingCategoryId,
                List.of(new CreateBusinessLocationRequest(
                        12.0,
                        77.0,
                        "Main Street",
                        null,
                        "Chennai",
                        "Tamil Nadu",
                        "India",
                        "600001")));

        when(businessCategoryRepository.findById(missingCategoryId))
                .thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> businessService.create(request))
                .isInstanceOf(BusinessCategoryNotFoundException.class)
                .hasMessageContaining("Business category not found with ID");
    }

    @Test
    void shouldThrowWhenBusinessDoesNotExist() {
        UUID businessId = UUID.randomUUID();
        when(businessRepository.findById(businessId)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> businessService.getById(businessId))
                .isInstanceOf(BusinessNotFoundException.class)
                .hasMessageContaining("Business not found with ID");

        assertThatThrownBy(() -> businessService.deactivate(businessId))
                .isInstanceOf(BusinessNotFoundException.class)
                .hasMessageContaining("Business not found with ID");
    }

    @Test
    void shouldReturnBusinessById() {
        UUID businessId = UUID.randomUUID();
        Business business = Business.builder()
                .id(businessId)
                .name("Central Cafe")
                .description("Coffee and desserts")
                .category(category)
                .status(BusinessStatus.ACTIVE)
                .version(2L)
                .locations(new ArrayList<>())
                .build();

        BusinessLocation location = BusinessLocation.builder()
                .id(UUID.randomUUID())
                .business(business)
                .latitude(12.9716)
                .longitude(77.5946)
                .addressLine1("Brigade Road")
                .city("Bengaluru")
                .state("Karnataka")
                .country("India")
                .postalCode("560001")
                .status(LocationStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        business.addLocation(location);

        when(businessRepository.findById(businessId)).thenReturn(java.util.Optional.of(business));

        BusinessResponse response = businessService.getById(businessId);

        assertThat(response.id()).isEqualTo(businessId);
        assertThat(response.name()).isEqualTo("Central Cafe");
        assertThat(response.categoryId()).isEqualTo(category.getId());
        assertThat(response.locations()).hasSize(1);
        assertThat(response.locations().get(0).city()).isEqualTo("Bengaluru");
    }

    @Test
    void shouldUpdateBusinessNameAndLocation() {
        UUID businessId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();

        Business business = Business.builder()
                .id(businessId)
                .name("Old Name")
                .description("Old description")
                .category(category)
                .status(BusinessStatus.ACTIVE)
                .version(0L)
                .locations(new ArrayList<>())
                .build();

        BusinessLocation existingLocation = BusinessLocation.builder()
                .id(locationId)
                .business(business)
                .latitude(10.0)
                .longitude(20.0)
                .addressLine1("Old address")
                .city("Old city")
                .state("Old state")
                .country("Old country")
                .postalCode("000000")
                .status(LocationStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        business.addLocation(existingLocation);

        when(businessRepository.findById(businessId)).thenReturn(java.util.Optional.of(business));

        UpdateBusinessRequest request = new UpdateBusinessRequest(
                "  Updated Name  ",
                "  Updated description  ",
                List.of(new UpdateBusinessLocationRequest(
                        locationId,
                        15.5,
                        25.5,
                        "New address",
                        "Suite 7",
                        "New city",
                        "New state",
                        "New country",
                        "111111")));

        BusinessResponse response = businessService.update(businessId, request);

        assertThat(response.name()).isEqualTo("Updated Name");
        assertThat(response.description()).isEqualTo("Updated description");
        assertThat(response.locations()).hasSize(1);
        assertThat(response.locations().get(0).addressLine1()).isEqualTo("New address");
        assertThat(response.locations().get(0).city()).isEqualTo("New city");
        assertThat(response.locations().get(0).postalCode()).isEqualTo("111111");
    }

    @Test
    void shouldChangeBusinessStatusForLifecycleActions() {
        UUID businessId = UUID.randomUUID();
        Business business = Business.builder()
                .id(businessId)
                .name("Status Test")
                .description("Lifecycle")
                .category(category)
                .status(BusinessStatus.ACTIVE)
                .version(0L)
                .locations(new ArrayList<>())
                .build();

        when(businessRepository.findById(businessId)).thenReturn(java.util.Optional.of(business));

        assertDoesNotThrow(() -> businessService.deactivate(businessId));
        assertThat(business.getStatus()).isEqualTo(BusinessStatus.INACTIVE);

        when(businessRepository.findById(businessId)).thenReturn(java.util.Optional.of(business));
        assertDoesNotThrow(() -> businessService.suspend(businessId));
        assertThat(business.getStatus()).isEqualTo(BusinessStatus.SUSPENDED);

        when(businessRepository.findById(businessId)).thenReturn(java.util.Optional.of(business));
        assertDoesNotThrow(() -> businessService.activate(businessId));
        assertThat(business.getStatus()).isEqualTo(BusinessStatus.ACTIVE);
    }
}
