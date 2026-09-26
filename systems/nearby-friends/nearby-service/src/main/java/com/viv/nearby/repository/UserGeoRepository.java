package com.viv.nearby.repository;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class UserGeoRepository {

        private static final String GEO_KEY = "nearby:users";

        private final RedisTemplate<String, String> redisTemplate;

        public UserGeoRepository(RedisTemplate<String, String> redisTemplate) {
                this.redisTemplate = redisTemplate;
        }

        public Point getUserLocation(String userId) {

                List<Point> positions = redisTemplate.opsForGeo()
                                .position(GEO_KEY, userId);

                if (positions == null || positions.isEmpty()) {
                        return null;
                }

                return positions.getFirst();
        }

        public List<GeoResult<RedisGeoCommands.GeoLocation<String>>> findNearby(
                        double latitude,
                        double longitude,
                        double radiusKm) {

                GeoOperations<String, String> geo = redisTemplate.opsForGeo();

                Circle circle = new Circle(
                                new Point(longitude, latitude),
                                new Distance(radiusKm, Metrics.KILOMETERS));

                GeoResults<RedisGeoCommands.GeoLocation<String>> results = geo.radius(
                                GEO_KEY,
                                circle,
                                RedisGeoCommands.GeoRadiusCommandArgs
                                                .newGeoRadiusArgs()
                                                .includeDistance()
                                                .sortAscending());

                if (results == null) {
                        return List.of();
                }

                return results.getContent();
        }
}