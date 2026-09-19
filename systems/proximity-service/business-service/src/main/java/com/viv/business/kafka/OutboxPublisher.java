package com.viv.business.kafka;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.viv.business.entity.OutboxEvent;
import com.viv.business.enums.OutboxEventStatus;
import com.viv.business.repository.OutboxEventRepository;
import com.viv.business.service.OutboxClaimService;
import com.viv.business.service.PublisherIdentity;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {

    private static final int BATCH_SIZE = 100;

    private final OutboxClaimService claimService;
    private final BusinessEventPublisher eventPublisher;
    private final OutboxEventRepository repository;
    private final PublisherIdentity publisherIdentity;

    @Scheduled(fixedDelay = 1000)
    public void publish() {

        String publisherId = publisherIdentity.getId();

        List<OutboxEvent> events = claimService.claimBatch(
                publisherId,
                BATCH_SIZE);

        for (OutboxEvent event : events) {

            try {

                eventPublisher
                        .publish(event)
                        .whenComplete(
                                (result, exception) -> {

                                    if (exception == null) {
                                        markPublished(
                                                event.getId());
                                    } else {
                                        markFailed(
                                                event.getId(),
                                                exception);
                                    }
                                });

            } catch (Exception e) {

                markFailed(
                        event.getId(),
                        e);
            }
        }
    }

    @Transactional
    protected void markPublished(UUID eventId) {

        repository.findById(eventId)
                .ifPresent(event -> {

                    event.setStatus(
                            OutboxEventStatus.PUBLISHED);

                    event.setPublishedAt(
                            Instant.now());

                    event.setClaimedAt(null);
                    event.setClaimedBy(null);
                    event.setLastError(null);
                });
    }

    @Transactional
    protected void markFailed(
            UUID eventId,
            Throwable exception) {

        repository.findById(eventId)
                .ifPresent(event -> {

                    event.setStatus(
                            OutboxEventStatus.PENDING);

                    event.setRetryCount(
                            event.getRetryCount() + 1);

                    event.setClaimedAt(null);
                    event.setClaimedBy(null);

                    event.setLastError(
                            exception.getMessage());
                });
    }

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void recoverStaleEvents() {

        Instant cutoff = Instant.now().minus(Duration.ofMinutes(5));

        repository.releaseStaleEvents(
                OutboxEventStatus.PROCESSING,
                OutboxEventStatus.PENDING,
                cutoff);
    }
}