package com.viv.location.service;

import org.springframework.stereotype.Service;

import com.viv.location.model.LocationUpdateRequest;
import com.viv.location.model.LocationUpdatedEvent;
import com.viv.location.publisher.LocationEventPublisher;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class LocationService {

    LocationEventPublisher locationEventPublisher;

    public void updateLocation(LocationUpdateRequest request) {

        LocationUpdatedEvent event = new LocationUpdatedEvent(
                request.userId(),
                request.latitude(),
                request.longitude(),
                request.accuracyMeters(),
                request.timestamp());

        locationEventPublisher.publish(event);
    }

}
