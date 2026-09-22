package com.viv.proximity.repository;

import java.util.Set;
import java.util.UUID;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import com.viv.proximity.redis.RedisKeyBuilder;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class BusinessLocationIndexRepository {

    private final RedisTemplate<String, String> redisTemplate;

    public void add(UUID businessId, UUID locationId) {

        redisTemplate.opsForSet().add(
                RedisKeyBuilder.businessLocations(businessId),
                locationId.toString());
    }

    public void remove(UUID businessId, UUID locationId) {

        redisTemplate.opsForSet().remove(
                RedisKeyBuilder.businessLocations(businessId),
                locationId.toString());
    }

    public Set<String> findLocationIds(UUID businessId) {

        Set<String> locationIds =
                redisTemplate.opsForSet().members(
                        RedisKeyBuilder.businessLocations(businessId));

        return locationIds == null
                ? Set.of()
                : locationIds;
    }

    public void delete(UUID businessId) {

        redisTemplate.delete(
                RedisKeyBuilder.businessLocations(businessId));
    }
}