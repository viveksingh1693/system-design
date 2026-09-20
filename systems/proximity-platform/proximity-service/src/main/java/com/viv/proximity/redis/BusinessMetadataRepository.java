package com.viv.proximity.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class BusinessMetadataRepository {

    private final RedisTemplate<String, String> redisTemplate;

    public void save(
            UUID businessId,
            UUID locationId,
            UUID categoryId,
            String name,
            String status,
            double latitude,
            double longitude) {

        String key =
                RedisKeyBuilder.business(businessId);

        Map<String, String> metadata = Map.of(
                "businessId", businessId.toString(),
                "locationId", locationId.toString(),
                "categoryId", categoryId.toString(),
                "name", name,
                "status", status,
                "latitude", String.valueOf(latitude),
                "longitude", String.valueOf(longitude)
        );

        redisTemplate.opsForHash()
                .putAll(key, metadata);
    }

    public Map<Object, Object> find(UUID businessId) {

        return redisTemplate.opsForHash()
                .entries(
                        RedisKeyBuilder.business(businessId)
                );
    }

    public void updateStatus(
            UUID businessId,
            String status) {

        redisTemplate.opsForHash()
                .put(
                        RedisKeyBuilder.business(businessId),
                        "status",
                        status
                );
    }

    public void delete(UUID businessId) {

        redisTemplate.delete(
                RedisKeyBuilder.business(businessId)
        );
    }
}