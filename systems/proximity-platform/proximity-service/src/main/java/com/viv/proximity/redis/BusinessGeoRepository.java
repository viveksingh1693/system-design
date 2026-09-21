package com.viv.proximity.redis;

import java.util.List;
import java.util.UUID;

import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.connection.RedisGeoCommands.GeoLocation;
import org.springframework.data.redis.connection.RedisGeoCommands.GeoSearchCommandArgs;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class BusinessGeoRepository {

        private final RedisTemplate<String, String> redisTemplate;

        private static final String GEO_KEY = RedisKeyBuilder.businessGeoIndex();

        public void add(
                        String businessId,
                        double latitude,
                        double longitude) {

                redisTemplate.opsForGeo().add(
                                GEO_KEY,
                                new Point(longitude, latitude),
                                businessId);
        }

        public void remove(String businessId) {

                redisTemplate.opsForZSet()
                                .remove(GEO_KEY, businessId);
        }

        public List<GeoResult<RedisGeoCommands.GeoLocation<String>>> findNearby(
                        double latitude,
                        double longitude,
                        double radiusMeters,
                        int limit) {

                Point center = new Point(longitude, latitude);

                Distance distance = new Distance(
                                radiusMeters,
                                RedisGeoCommands.DistanceUnit.METERS);

                GeoSearchCommandArgs args = RedisGeoCommands.GeoSearchCommandArgs
                                .newGeoSearchArgs()
                                .includeDistance()
                                .sortAscending()
                                .limit(limit);

                GeoResults<GeoLocation<String>> results = redisTemplate.opsForGeo().search(
                                GEO_KEY,
                                org.springframework.data.redis.domain.geo.GeoReference
                                                .fromCoordinate(center),
                                org.springframework.data.redis.domain.geo.GeoShape
                                                .byRadius(distance),
                                args);

                if (results == null) {
                        return List.of();
                }

                return results.getContent();
        }

        public void remove(UUID businessId) {
                redisTemplate.opsForZSet().remove(
                                RedisKeyBuilder.businessGeoIndex(),
                                businessId.toString());
        }

        public void add(
                        UUID businessId,
                        double latitude,
                        double longitude) {

                redisTemplate.opsForGeo().add(
                                RedisKeyBuilder.businessGeoIndex(),
                                new Point(longitude, latitude),
                                businessId.toString());
        }
}