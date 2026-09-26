package com.viv.location.repository;

import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class UserGeoRepository {

    private static final String GEO_KEY = "nearby:users";

    private final RedisTemplate<String, String> redisTemplate;

    public void updateLocation(
            String userId, double latitude, double longitude) {
        redisTemplate.opsForGeo()
        .add(GEO_KEY, new Point(longitude, latitude), userId);
    }

}
