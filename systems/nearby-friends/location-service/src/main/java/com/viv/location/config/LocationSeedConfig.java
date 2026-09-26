package com.viv.location.config;
import com.viv.location.model.LocationUpdateRequest;
import com.viv.location.service.LocationService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Instant;
import java.util.List;

@Configuration
public class LocationSeedConfig {

    @Bean
    CommandLineRunner seedLocations(LocationService locationService) {
        return args -> {

            List<LocationUpdateRequest> locations = List.of(
                    new LocationUpdateRequest("u1", 28.4595, 77.0266, 8.5, Instant.now()),
                    new LocationUpdateRequest("u2", 28.4610, 77.0300, 7.2, Instant.now()),
                    new LocationUpdateRequest("u3", 28.4625, 77.0350, 9.1, Instant.now()),
                    new LocationUpdateRequest("u4", 28.4700, 77.0400, 6.8, Instant.now()),
                    new LocationUpdateRequest("u5", 28.4800, 77.0500, 10.0, Instant.now()),
                    new LocationUpdateRequest("u6", 28.5000, 77.0600, 11.5, Instant.now()),
                    new LocationUpdateRequest("u7", 28.4300, 77.0000, 8.0, Instant.now()),
                    new LocationUpdateRequest("u8", 28.4550, 77.0240, 7.5, Instant.now()),
                    new LocationUpdateRequest("u9", 28.4580, 77.0290, 6.5, Instant.now()),
                    new LocationUpdateRequest("u10", 28.5200, 77.0800, 9.5, Instant.now()));

            locations.forEach(locationService::updateLocation);

            System.out.println(
                    "Seeded " + locations.size() + " user locations");
        };
    }
}
