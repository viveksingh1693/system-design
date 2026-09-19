package com.viv.business.service;
import lombok.RequiredArgsConstructor;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Service;

import com.viv.business.entity.OutboxEvent;
import com.viv.business.enums.OutboxEventStatus;
import com.viv.business.repository.OutboxEventRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxEventRepository repository;

    private final ObjectMapper objectMapper;

    public void save(
            String aggregateType,
            UUID aggregateId,
            String eventType,
            Object event) {

        try {

            String payload =
                    objectMapper.writeValueAsString(event);

            OutboxEvent outboxEvent =
                    OutboxEvent.builder()
                            .aggregateType(aggregateType)
                            .aggregateId(aggregateId)
                            .eventType(eventType)
                            .payload(payload)
                            .status(
                                    OutboxEventStatus.PENDING
                            )
                            .retryCount(0)
                            .build();

            repository.save(outboxEvent);

        } catch (JacksonException exception) {

            throw new IllegalStateException(
                    "Unable to serialize outbox event",
                    exception
            );
        }
    }
}