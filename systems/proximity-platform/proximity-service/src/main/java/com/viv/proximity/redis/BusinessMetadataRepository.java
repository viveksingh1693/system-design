package com.viv.proximity.redis;

import java.util.Map;
import java.util.UUID;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import com.viv.proximity.dto.NearbyBusinessResponse;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class BusinessMetadataRepository {

        private final RedisTemplate<String, String> redisTemplate;

        public Map<Object, Object> find(UUID businessId) {

                return redisTemplate.opsForHash()
                                .entries(
                                                RedisKeyBuilder.business(businessId));
        }

        public NearbyBusinessResponse findBusiness(
                        UUID businessId,
                        double distanceMeters) {

                Map<Object, Object> data = find(businessId);

                if (data == null || data.isEmpty()) {
                        return null;
                }

                return new NearbyBusinessResponse(
                                UUID.fromString(
                                                value(data, "businessId")),
                                UUID.fromString(
                                                value(data, "locationId")),
                                UUID.fromString(
                                                value(data, "categoryId")),
                                value(data, "name"),
                                value(data, "status"),
                                Double.parseDouble(
                                                value(data, "latitude")),
                                Double.parseDouble(
                                                value(data, "longitude")),
                                distanceMeters);
        }

        private String value(
                        Map<Object, Object> data,
                        String key) {

                Object value = data.get(key);

                return value == null
                                ? null
                                : value.toString();
        }

        public void updateStatus(UUID businessId, String status) {
                redisTemplate.opsForHash().put(
                                RedisKeyBuilder.business(businessId),
                                "status",
                                status);
        }

        public void save(
                        UUID businessId,
                        UUID locationId,
                        UUID categoryId,
                        String name,
                        String status,
                        double latitude,
                        double longitude) {

                String key = RedisKeyBuilder.business(businessId);

                Map<String, String> metadata = Map.of(
                                "businessId", businessId.toString(),
                                "locationId", locationId.toString(),
                                "categoryId", categoryId.toString(),
                                "name", name,
                                "status", status,
                                "latitude", String.valueOf(latitude),
                                "longitude", String.valueOf(longitude));

                redisTemplate.opsForHash().putAll(key, metadata);
        }
}