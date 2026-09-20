package com.viv.business.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.viv.business.entity.OutboxEvent;
import com.viv.business.enums.OutboxEventStatus;
import com.viv.business.repository.OutboxEventRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxClaimService {

    private final OutboxEventRepository repository;

    @Transactional
    public List<OutboxEvent> claimBatch(
            String publisherId,
            int batchSize) {

        List<OutboxEvent> events =
                repository.findPendingForUpdate(
                        OutboxEventStatus.PENDING,
                        PageRequest.of(0, batchSize)
                );

        if (events.isEmpty()) {
            return events;
        }

        Instant now = Instant.now();

        for (OutboxEvent event : events) {
            event.setStatus(OutboxEventStatus.PROCESSING);
            event.setClaimedAt(now);
            event.setClaimedBy(publisherId);
        }

        log.debug(
                "Claimed {} outbox events. publisherId={}",
                events.size(),
                publisherId
        );

        return events;
    }

    @Transactional
    public int markPublished(
            UUID eventId,
            String publisherId,
            Instant publishedAt) {

        return repository.markPublished(
                eventId,
                publisherId,
                publishedAt,
                OutboxEventStatus.PROCESSING,
                OutboxEventStatus.PUBLISHED
        );
    }

    @Transactional
    public int markFailed(
            UUID eventId,
            String publisherId,
            Throwable exception) {

        String errorMessage = exception.getMessage();

        if (errorMessage == null || errorMessage.isBlank()) {
            errorMessage = exception.getClass().getName();
        }

        if (errorMessage.length() > 2000) {
            errorMessage = errorMessage.substring(0, 2000);
        }

        return repository.markFailed(
                eventId,
                publisherId,
                errorMessage,
                OutboxEventStatus.PROCESSING,
                OutboxEventStatus.PENDING
        );
    }

    @Transactional
    public int releaseStaleEvents(Instant cutoff) {

        return repository.releaseStaleEvents(
                OutboxEventStatus.PROCESSING,
                OutboxEventStatus.PENDING,
                cutoff
        );
    }
}