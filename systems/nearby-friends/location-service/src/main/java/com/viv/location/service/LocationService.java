package com.viv.location.service;

import org.springframework.stereotype.Service;

import com.viv.location.model.LocationUpdateRequest;
import com.viv.location.model.LocationUpdatedEvent;
import com.viv.location.publisher.LocationEventPublisher;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j 
@Service
@AllArgsConstructor
public class LocationService {

    LocationEventPublisher locationEventPublisher;

    public void updateLocation(LocationUpdateRequest request) {

        log.info("Updating location: {}" ,request);
        LocationUpdatedEvent event = new LocationUpdatedEvent(
                request.userId(),
                request.latitude(),
                request.longitude(),
                request.accuracyMeters(),
                request.timestamp());

        locationEventPublisher.publish(event);
    }

}
