package com.viv.proximity.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class BusinessGeoRepository {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String GEO_KEY =
            RedisKeyBuilder.businessGeoIndex();

    public void add(
            UUID businessId,
            double latitude,
            double longitude) {

        redisTemplate.opsForGeo().add(
                GEO_KEY,
                new Point(longitude, latitude),
                businessId.toString()
        );
    }

    public void remove(UUID businessId) {
        redisTemplate.opsForGeo().remove(
                GEO_KEY,
                businessId.toString()
        );
    }

    public GeoResults<RedisGeoCommands.GeoLocation<String>> findNearby(
            double latitude,
            double longitude,
            double radiusKm) {

        Point center = new Point(longitude, latitude);
        Distance distance = new Distance(radiusKm, RedisGeoCommands.DistanceUnit.KILOMETERS);

        return redisTemplate.opsForGeo().search(
                GEO_KEY,
                GeoReference.fromCoordinate(center),
                distance
        );
    }

}