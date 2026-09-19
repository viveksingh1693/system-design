package com.viv.business.kafka;

import java.util.concurrent.CompletableFuture;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.viv.business.entity.OutboxEvent;
import com.viv.business.event.BusinessEventEnvelope;

import lombok.RequiredArgsConstructor;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import org.springframework.kafka.support.SendResult;

@Service
@RequiredArgsConstructor
public class BusinessEventPublisher {

    private static final String TOPIC = "business-events";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public CompletableFuture<SendResult<String, String>> publish(
            OutboxEvent event) {

        try {
            JsonNode payload = objectMapper.readTree(event.getPayload());

            BusinessEventEnvelope envelope = new BusinessEventEnvelope(
                    event.getId(),
                    event.getEventType(),
                    event.getAggregateType(),
                    event.getAggregateId(),
                    1,
                    event.getCreatedAt(),
                    payload);

            String message = objectMapper.writeValueAsString(envelope);

            return kafkaTemplate.send(
                    TOPIC,
                    event.getAggregateId().toString(),
                    message);

        } catch (JacksonException e) {
            throw new IllegalStateException(
                    "Unable to serialize event: "
                            + event.getId(),
                    e);
        }
    }
}