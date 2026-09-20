package com.viv.proximity.consumer;
import com.viv.proximity.model.BusinessEventEnvelope;
import com.viv.proximity.redis.ProcessedEventRepository;
import com.viv.proximity.service.BusinessGeoIndexService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class BusinessEventConsumer {

    private final ObjectMapper objectMapper;
    private final ProcessedEventRepository processedEventRepository;
    private final BusinessGeoIndexService geoIndexService;

    @KafkaListener(
            topics = "business-events",
            groupId = "proximity-service"
    )
    public void consume(
            String message,
            Acknowledgment acknowledgment) {

        BusinessEventEnvelope event;

        /*
         * ---------------------------------------------------------
         * 1. Deserialize Kafka message
         * ---------------------------------------------------------
         */
        try {

            event = objectMapper.readValue(
                    message,
                    BusinessEventEnvelope.class
            );

        } catch (Exception e) {

            log.error(
                    "Failed to deserialize business event. message={}",
                    message,
                    e
            );

            /*
             * Do NOT acknowledge the message.
             *
             * Kafka will consider the message unprocessed.
             *
             * Later we should add a Dead Letter Topic (DLT)
             * for permanently malformed messages.
             */
            throw new IllegalArgumentException(
                    "Invalid business event message",
                    e
            );
        }

        log.info(
                "Received business event. eventId={}, eventType={}, aggregateType={}, aggregateId={}",
                event.eventId(),
                event.eventType(),
                event.aggregateType(),
                event.aggregateId()
        );

        /*
         * ---------------------------------------------------------
         * 2. Check if event was already successfully processed
         * ---------------------------------------------------------
         *
         * This handles Kafka's at-least-once delivery semantics.
         *
         * Example:
         *
         * Kafka
         *   ↓
         * Consumer
         *   ↓
         * Redis updated
         *   ↓
         * Consumer crashes before ACK
         *   ↓
         * Kafka redelivers
         *   ↓
         * isProcessed() == true
         *   ↓
         * Ignore duplicate
         */
        if (processedEventRepository.isProcessed(
                event.eventId())) {

            log.info(
                    "Ignoring already processed event. eventId={}, eventType={}",
                    event.eventId(),
                    event.eventType()
            );

            acknowledgment.acknowledge();

            return;
        }

        /*
         * ---------------------------------------------------------
         * 3. Try to claim the event
         * ---------------------------------------------------------
         *
         * This should use an atomic Redis SET NX operation.
         *
         * Only one consumer should be allowed to process
         * the event at a time.
         */
        if (!processedEventRepository.tryClaim(
                event.eventId())) {

            log.info(
                    "Event is currently being processed by another consumer. " +
                    "eventId={}, eventType={}",
                    event.eventId(),
                    event.eventType()
            );

            /*
             * IMPORTANT:
             *
             * We don't acknowledge here.
             *
             * The other consumer owns the event.
             *
             * Depending on the exact consumer topology, this branch
             * may be uncommon because a Kafka partition is normally
             * assigned to only one consumer within the same group.
             */
            return;
        }

        /*
         * ---------------------------------------------------------
         * 4. Process event
         * ---------------------------------------------------------
         */
        try {

            log.info(
                    "Processing business event. eventId={}, eventType={}",
                    event.eventId(),
                    event.eventType()
            );

            geoIndexService.handle(event);

            /*
             * -----------------------------------------------------
             * 5. Mark event as successfully processed
             * -----------------------------------------------------
             *
             * This happens BEFORE Kafka acknowledgment.
             */
            processedEventRepository.markProcessed(
                    event.eventId()
            );

            /*
             * -----------------------------------------------------
             * 6. Acknowledge Kafka message
             * -----------------------------------------------------
             */
            acknowledgment.acknowledge();

            log.info(
                    "Successfully processed business event. " +
                    "eventId={}, eventType={}, aggregateId={}",
                    event.eventId(),
                    event.eventType(),
                    event.aggregateId()
            );

        } catch (Exception e) {

            log.error(
                    "Failed to process business event. " +
                    "eventId={}, eventType={}, aggregateId={}",
                    event.eventId(),
                    event.eventType(),
                    event.aggregateId(),
                    e
            );

            /*
             * -----------------------------------------------------
             * 7. Release processing claim
             * -----------------------------------------------------
             *
             * This allows the Kafka message to be retried.
             */
            processedEventRepository.release(
                    event.eventId()
            );

            /*
             * Do NOT acknowledge.
             *
             * Throwing the exception causes Spring Kafka to treat
             * the message as failed.
             */
            throw e;
        }
    }
}