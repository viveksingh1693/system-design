package com.viv.processor.service;

import java.time.Instant;
import java.util.Set;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
@Service
public class LocationExpiryService {

    private static final String GEO_KEY = "nearby:user:geo";

    private static final String LAST_SEEN_KEY = "nearby:user:last-seen";

    /**
     * A user's location is considered expired if
     * it has not been updated for 2 minutes.
     */
    private static final long LOCATION_TTL_MILLIS = 2 * 60 * 1000L;

    /**
     * Run expiry check every 30 seconds.
     */
    private static final long EXPIRY_INTERVAL_MILLIS = 30_000L;

    private final RedisTemplate<String, String> redisTemplate;

    @Scheduled(fixedDelay = EXPIRY_INTERVAL_MILLIS)
    public void removeExpiredLocations() {

        /*
         * Redis stores last-seen timestamps as epoch milliseconds.
         *
         * Epoch time is timezone-independent, so there is no
         * IST/UTC conversion required here.
         */
        long now = Instant.now().toEpochMilli();

        long cutoff = now - LOCATION_TTL_MILLIS;

        log.debug(
                "Location expiry check: now={}, nowInstant={}, cutoff={}, cutoffInstant={}",
                now,
                Instant.ofEpochMilli(now),
                cutoff,
                Instant.ofEpochMilli(cutoff)
        );

        /*
         * Find users whose last location update is older than
         * the configured TTL.
         */
        Set<String> expiredUsers =
                redisTemplate.opsForZSet()
                        .rangeByScore(
                                LAST_SEEN_KEY,
                                0,
                                cutoff
                        );

        if (expiredUsers == null || expiredUsers.isEmpty()) {

            log.debug("No expired locations found.");

            return;
        }

        log.info(
                "Found {} potentially expired users",
                expiredUsers.size()
        );

        for (String userId : expiredUsers) {

            /*
             * Re-read the timestamp before deleting.
             *
             * This protects against a race:
             *
             * 1. Expiry service finds u1 as expired
             * 2. u1 sends a new location
             * 3. Expiry service tries to delete u1
             *
             * If the timestamp has changed, we must NOT delete it.
             */
            Double lastSeen =
                    redisTemplate.opsForZSet()
                            .score(
                                    LAST_SEEN_KEY,
                                    userId
                            );

            /*
             * The user may already have been removed by another
             * expiry execution/instance.
             */
            if (lastSeen == null) {

                log.debug(
                        "Skipping user={} because last-seen entry no longer exists",
                        userId
                );

                continue;
            }

            /*
             * Location was refreshed after the initial query.
             */
            if (lastSeen > cutoff) {

                log.debug(
                        "Skipping user={} because location was refreshed. lastSeen={}, lastSeenInstant={}",
                        userId,
                        lastSeen.longValue(),
                        Instant.ofEpochMilli(lastSeen.longValue())
                );

                continue;
            }

            /*
             * Remove user from the Redis GEO proximity index.
             */
            Boolean geoRemoved =
                    redisTemplate.opsForGeo()
                            .remove(
                                    GEO_KEY,
                                    userId
                            );

            /*
             * Remove the user's last-seen entry.
             */
            Long lastSeenRemoved =
                    redisTemplate.opsForZSet()
                            .remove(
                                    LAST_SEEN_KEY,
                                    userId
                            );

            log.info(
                    "Expired location removed: user={}, lastSeen={}, lastSeenInstant={}, ageMs={}, geoRemoved={}, lastSeenRemoved={}",
                    userId,
                    lastSeen.longValue(),
                    Instant.ofEpochMilli(lastSeen.longValue()),
                    now - lastSeen.longValue(),
                    geoRemoved,
                    lastSeenRemoved
            );
        }
    }
}