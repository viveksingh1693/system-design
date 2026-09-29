package com.viv.processor.repository;

import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import com.viv.processor.model.LocationUpdatedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j 
@Repository 
@RequiredArgsConstructor 
public class UserGeoRepository {

    private static final String GEO_KEY = "nearby:user:geo";
    private static final String LAST_SEEN_KEY = "nearby:user:last-seen";

    private final RedisTemplate<String, String> redisTemplate;

    public boolean updateLocation(LocationUpdatedEvent event) {


        String userId = event.userId();
        long timestamp = event.timestamp().toEpochMilli();
        log.info("Updating location for user: {}, timestamp: {}", userId, timestamp);

        Double existingTimestamp =
                redisTemplate.opsForZSet()
                        .score(LAST_SEEN_KEY, userId);

        if (existingTimestamp != null
                && timestamp <= existingTimestamp) {
            log.info("Skipping location update for user: {}, timestamp: {}", userId, timestamp);
            return false;
        }

        redisTemplate.opsForGeo()
                .add(
                        GEO_KEY,
                        new Point(
                                event.longitude(),
                                event.latitude()
                        ),
                        userId
                );

        redisTemplate.opsForZSet()
                .add(
                        LAST_SEEN_KEY,
                        userId,
                        timestamp
                );

        log.info("Location updated for user: {}, timestamp: {}", userId, timestamp);
        return true;
    }
}