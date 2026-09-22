package com.viv.proximity.redis;

import java.util.Map;
import java.util.UUID;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class BusinessLocationMetadataRepository {

    private final RedisTemplate<String, String> redisTemplate;

    public void save(
            UUID businessId,
            UUID locationId,
            UUID categoryId,
            String name,
            String status,
            double latitude,
            double longitude) {

        String key = RedisKeyBuilder.businessLocation(locationId);

        Map<String, String> metadata = Map.of(
                "businessId", businessId.toString(),
                "locationId", locationId.toString(),
                "categoryId", categoryId.toString(),
                "name", name,
                "status", status,
                "latitude", String.valueOf(latitude),
                "longitude", String.valueOf(longitude));

        redisTemplate.opsForHash()
                .putAll(key, metadata);
    }

    public Map<Object, Object> find(UUID locationId) {

        return redisTemplate.opsForHash()
                .entries(
                        RedisKeyBuilder.businessLocation(locationId));
    }

    public void updateStatus(
            UUID locationId,
            String status) {

        redisTemplate.opsForHash()
                .put(
                        RedisKeyBuilder.businessLocation(locationId),
                        "status",
                        status);
    }

    public void delete(UUID locationId) {

        redisTemplate.delete(
                RedisKeyBuilder.businessLocation(locationId));
    }
}