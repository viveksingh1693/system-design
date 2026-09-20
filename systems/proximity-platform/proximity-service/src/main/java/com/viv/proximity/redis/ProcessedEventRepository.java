package com.viv.proximity.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ProcessedEventRepository {

    private static final Duration PROCESSING_TTL =
            Duration.ofMinutes(5);

    private static final Duration PROCESSED_TTL =
            Duration.ofDays(7);

    private final RedisTemplate<String, String> redisTemplate;

    public boolean isProcessed(UUID eventId) {

        return Boolean.TRUE.equals(
                redisTemplate.hasKey(
                        RedisKeyBuilder.processedEvent(eventId)
                )
        );
    }

    public boolean tryClaim(UUID eventId) {

        if (isProcessed(eventId)) {
            return false;
        }

        Boolean claimed =
                redisTemplate.opsForValue().setIfAbsent(
                        processingKey(eventId),
                        "1",
                        PROCESSING_TTL
                );

        return Boolean.TRUE.equals(claimed);
    }

    public void markProcessed(UUID eventId) {

        redisTemplate.delete(
                processingKey(eventId)
        );

        redisTemplate.opsForValue().set(
                RedisKeyBuilder.processedEvent(eventId),
                "1",
                PROCESSED_TTL
        );
    }

    public void release(UUID eventId) {

        redisTemplate.delete(
                processingKey(eventId)
        );
    }

    private String processingKey(UUID eventId) {

        return "processing:event:" + eventId;
    }
}