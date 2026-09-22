package com.viv.proximity.redis;

import java.util.List;
import java.util.UUID;

import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.connection.RedisGeoCommands.GeoSearchCommandArgs;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.data.redis.domain.geo.GeoShape;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class BusinessGeoRepository {

    private static final String GEO_KEY =
            RedisKeyBuilder.businessGeoIndex();

    private final RedisTemplate<String, String> redisTemplate;

    public void add(
            UUID locationId,
            double latitude,
            double longitude) {

        redisTemplate.opsForGeo().add(
                GEO_KEY,
                new Point(longitude, latitude),
                locationId.toString());
    }

    public void remove(UUID locationId) {

        redisTemplate.opsForZSet().remove(
                GEO_KEY,
                locationId.toString());
    }

    public List<GeoResult<RedisGeoCommands.GeoLocation<String>>> findNearby(
            double latitude,
            double longitude,
            double radiusMeters,
            int limit) {

        Point center =
                new Point(longitude, latitude);

        Distance distance =
                new Distance(
                        radiusMeters,
                        RedisGeoCommands.DistanceUnit.METERS);

        GeoSearchCommandArgs args =
                RedisGeoCommands.GeoSearchCommandArgs
                        .newGeoSearchArgs()
                        .includeDistance()
                        .sortAscending()
                        .limit(limit);

        GeoResults<RedisGeoCommands.GeoLocation<String>> results =
                redisTemplate.opsForGeo().search(
                        GEO_KEY,
                        GeoReference.fromCoordinate(center),
                        GeoShape.byRadius(distance),
                        args);

        if (results == null) {
            return List.of();
        }

        return results.getContent();
    }
}