package com.viv.business.service;

import java.time.Instant;
import java.util.List;

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

        // log.info("Claiming outbox events for publisherId: {} with batchSize: {}", publisherId, batchSize);

        List<OutboxEvent> events =
                repository.findPendingForUpdate(
                        OutboxEventStatus.PENDING,
                        PageRequest.of(0, batchSize)
                );

        // log.info("Found {} outbox events to claim for publisherId: {}", events.size(), publisherId);

        Instant now = Instant.now();

        for (OutboxEvent event : events) {
            event.setStatus(OutboxEventStatus.PROCESSING);
            event.setClaimedAt(now);
            event.setClaimedBy(publisherId);
        }

        // log.info("Marked {} outbox events as PROCESSING for publisherId: {}", events.size(), publisherId);

        return events;
    }
}