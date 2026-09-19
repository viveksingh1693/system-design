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

@Service 
@RequiredArgsConstructor 
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

        Instant now = Instant.now();

        for (OutboxEvent event : events) {
            event.setStatus(OutboxEventStatus.PROCESSING);
            event.setClaimedAt(now);
            event.setClaimedBy(publisherId);
        }

        return events;
    }
}