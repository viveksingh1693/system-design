package com.viv.business.kafka;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.viv.business.entity.OutboxEvent;
import com.viv.business.service.OutboxClaimService;
import com.viv.business.service.PublisherIdentity;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {

    private static final int BATCH_SIZE = 100;

    private static final Duration STALE_EVENT_TIMEOUT =
            Duration.ofMinutes(5);

    private final OutboxClaimService claimService;
    private final BusinessEventPublisher eventPublisher;
    private final PublisherIdentity publisherIdentity;

    /**
     * Polls pending outbox events, claims them and publishes them to Kafka.
     *
     * Runs every second.
     *
     * Important:
     * - Database transaction is used only while claiming events.
     * - Kafka publishing is asynchronous.
     * - Successful/failed callbacks update the event only if this
     *   publisher still owns the PROCESSING record.
     */
    @Scheduled(fixedDelay = 1000)
    public void publish() {

        String publisherId = publisherIdentity.getId();

        List<OutboxEvent> events =
                claimService.claimBatch(
                        publisherId,
                        BATCH_SIZE
                );

        if (events.isEmpty()) {
            return;
        }

        log.debug(
                "Claimed {} outbox events. publisherId={}",
                events.size(),
                publisherId
        );

        for (OutboxEvent event : events) {
            publishEvent(event, publisherId);
        }
    }

    /**
     * Publishes a single outbox event to Kafka.
     */
    private void publishEvent(
            OutboxEvent event,
            String publisherId) {

        try {

            log.debug(
                    "Publishing outbox event. eventId={}, eventType={}, aggregateId={}, publisherId={}",
                    event.getId(),
                    event.getEventType(),
                    event.getAggregateId(),
                    publisherId
            );

            eventPublisher
                    .publish(event)
                    .whenComplete((result, exception) -> {

                        if (exception == null) {

                            handlePublishSuccess(
                                    event,
                                    publisherId
                            );

                        } else {

                            handlePublishFailure(
                                    event,
                                    publisherId,
                                    exception
                            );
                        }
                    });

        } catch (Exception exception) {

            log.error(
                    "Unexpected error while publishing outbox event. " +
                    "eventId={}, eventType={}, aggregateId={}, publisherId={}",
                    event.getId(),
                    event.getEventType(),
                    event.getAggregateId(),
                    publisherId,
                    exception
            );

            handlePublishFailure(
                    event,
                    publisherId,
                    exception
            );
        }
    }

    /**
     * Handles successful Kafka publication.
     */
    private void handlePublishSuccess(
            OutboxEvent event,
            String publisherId) {

        try {

            int updatedRows =
                    claimService.markPublished(
                            event.getId(),
                            publisherId,
                            Instant.now()
                    );

            if (updatedRows == 1) {

                log.info(
                        "Outbox event published successfully. " +
                        "eventId={}, eventType={}, aggregateId={}, publisherId={}",
                        event.getId(),
                        event.getEventType(),
                        event.getAggregateId(),
                        publisherId
                );

            } else {

                /*
                 * This can happen if:
                 *
                 * 1. The event became stale.
                 * 2. Another publisher claimed it.
                 * 3. Another process already changed its state.
                 *
                 * We intentionally do not overwrite the newer state.
                 */
                log.warn(
                        "Outbox event was published to Kafka but ownership " +
                        "was lost before marking PUBLISHED. " +
                        "eventId={}, publisherId={}",
                        event.getId(),
                        publisherId
                );
            }

        } catch (Exception exception) {

            log.error(
                    "Failed to mark outbox event as PUBLISHED. " +
                    "eventId={}, publisherId={}",
                    event.getId(),
                    publisherId,
                    exception
            );
        }
    }

    /**
     * Handles Kafka publication failure.
     */
    private void handlePublishFailure(
            OutboxEvent event,
            String publisherId,
            Throwable exception) {

        try {

            int updatedRows =
                    claimService.markFailed(
                            event.getId(),
                            publisherId,
                            exception
                    );

            if (updatedRows == 1) {

                log.error(
                        "Failed to publish outbox event. " +
                        "eventId={}, eventType={}, aggregateId={}, retryCount will be incremented, publisherId={}",
                        event.getId(),
                        event.getEventType(),
                        event.getAggregateId(),
                        publisherId,
                        exception
                );

            } else {

                /*
                 * The event is no longer owned by this publisher.
                 *
                 * Do NOT modify it because another publisher may already
                 * be processing the same event.
                 */
                log.warn(
                        "Failed to publish outbox event, but ownership " +
                        "was already lost. eventId={}, publisherId={}",
                        event.getId(),
                        publisherId,
                        exception
                );
            }

        } catch (Exception updateException) {

            log.error(
                    "Failed to mark outbox event as PENDING after Kafka failure. " +
                    "eventId={}, publisherId={}",
                    event.getId(),
                    publisherId,
                    updateException
            );
        }
    }

    /**
     * Recovers events that were stuck in PROCESSING state.
     *
     * Example:
     *
     * Publisher A claims event
     *       |
     *       | application crashes
     *       v
     * PROCESSING
     *       |
     *       | > 5 minutes
     *       v
     * PENDING
     *
     * Another publisher can then claim the event.
     */
    @Scheduled(fixedDelay = 60_000)
    public void recoverStaleEvents() {

        Instant cutoff =
                Instant.now().minus(STALE_EVENT_TIMEOUT);

        try {

            int recovered =
                    claimService.releaseStaleEvents(cutoff);

            if (recovered > 0) {

                log.warn(
                        "Recovered stale outbox events. " +
                        "count={}, cutoff={}",
                        recovered,
                        cutoff
                );
            }

        } catch (Exception exception) {

            log.error(
                    "Failed to recover stale outbox events. cutoff={}",
                    cutoff,
                    exception
            );
        }
    }
}