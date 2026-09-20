package com.viv.business.kafka;

import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import com.viv.business.entity.OutboxEvent;
import com.viv.business.event.BusinessEventEnvelope;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@Slf4j
public class BusinessEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${business.events.topic:business-events}")
    private String topic;

    /**
     * Publishes an outbox event to Kafka.
     *
     * The returned CompletableFuture completes when Kafka
     * acknowledges or rejects the message.
     */
    public CompletableFuture<SendResult<String, String>> publish(
            OutboxEvent event) {

        try {

            String payload =
                    buildKafkaEnvelope(event);

            String kafkaKey =
                    event.getAggregateId().toString();

            log.info(
                    "Publishing business event. " +
                    "eventId={}, eventType={}, aggregateId={}, topic={}, key={}",
                    event.getId(),
                    event.getEventType(),
                    event.getAggregateId(),
                    topic,
                    kafkaKey
            );

            return kafkaTemplate.send(
                    topic,
                    kafkaKey,
                    payload
            );

        } catch (JacksonException e) {

            log.error(
                    "Failed to serialize business event. " +
                    "eventId={}, eventType={}, aggregateId={}",
                    event.getId(),
                    event.getEventType(),
                    event.getAggregateId(),
                    e
            );

            /*
             * Convert serialization failure into a failed
             * CompletableFuture so the caller can use the
             * same success/failure handling path.
             */
            CompletableFuture<SendResult<String, String>> failed =
                    new CompletableFuture<>();

            failed.completeExceptionally(e);

            return failed;
        }
    }

    /**
     * Builds the Kafka envelope.
     *
     * The payload stored in the outbox is already JSON.
     * We therefore parse it into JsonNode rather than
     * serializing it as an escaped JSON string.
     */
    private String buildKafkaEnvelope(
            OutboxEvent event)
            throws JacksonException {

        BusinessEventEnvelope envelope =
                new BusinessEventEnvelope(
                        event.getId(),
                        event.getEventType(),
                        event.getAggregateType(),
                        event.getAggregateId(),
                        1,
                        event.getCreatedAt(),
                        objectMapper.readTree(
                                event.getPayload()
                        )
                );

        return objectMapper.writeValueAsString(
                envelope
        );
    }
}