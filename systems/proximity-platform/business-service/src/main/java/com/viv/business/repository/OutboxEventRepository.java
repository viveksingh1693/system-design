package com.viv.business.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.viv.business.entity.OutboxEvent;
import com.viv.business.enums.OutboxEventStatus;

import jakarta.persistence.LockModeType;

@Repository
public interface OutboxEventRepository
        extends JpaRepository<OutboxEvent, UUID> {

    /**
     * Finds pending events ordered by creation time.
     */
    List<OutboxEvent> findByStatusOrderByCreatedAtAsc(
            OutboxEventStatus status,
            Pageable pageable);

    /**
     * Claims pending events using a pessimistic database lock.
     *
     * The lock is held for the duration of the transaction in
     * OutboxClaimService.claimBatch().
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT e
            FROM OutboxEvent e
            WHERE e.status = :status
            ORDER BY e.createdAt ASC
            """)
    List<OutboxEvent> findPendingForUpdate(
            @Param("status") OutboxEventStatus status,
            Pageable pageable);

    /**
     * Marks an event as successfully published.
     *
     * PROCESSING -> PUBLISHED
     *
     * The claimedBy check is important. It prevents an old asynchronous
     * Kafka callback from updating an event that has already been
     * reclaimed by another publisher.
     */
    @Modifying
    @Query("""
            UPDATE OutboxEvent e
               SET e.status = :published,
                   e.publishedAt = :publishedAt,
                   e.claimedAt = null,
                   e.claimedBy = null,
                   e.lastError = null
             WHERE e.id = :eventId
               AND e.status = :processing
               AND e.claimedBy = :publisherId
            """)
    int markPublished(
            @Param("eventId") UUID eventId,
            @Param("publisherId") String publisherId,
            @Param("publishedAt") Instant publishedAt,
            @Param("processing") OutboxEventStatus processing,
            @Param("published") OutboxEventStatus published);

    /**
     * Marks an event for retry after Kafka publication failure.
     *
     * PROCESSING -> PENDING
     *
     * retryCount is incremented atomically in the database.
     */
    @Modifying
    @Query("""
            UPDATE OutboxEvent e
               SET e.status = :pending,
                   e.retryCount = e.retryCount + 1,
                   e.claimedAt = null,
                   e.claimedBy = null,
                   e.lastError = :lastError
             WHERE e.id = :eventId
               AND e.status = :processing
               AND e.claimedBy = :publisherId
            """)
    int markFailed(
            @Param("eventId") UUID eventId,
            @Param("publisherId") String publisherId,
            @Param("lastError") String lastError,
            @Param("processing") OutboxEventStatus processing,
            @Param("pending") OutboxEventStatus pending);

    /**
     * Releases events that have been stuck in PROCESSING state.
     *
     * PROCESSING -> PENDING
     */
    @Modifying
    @Query("""
            UPDATE OutboxEvent e
               SET e.status = :pending,
                   e.claimedAt = null,
                   e.claimedBy = null
             WHERE e.status = :processing
               AND e.claimedAt < :cutoff
            """)
    int releaseStaleEvents(
            @Param("processing") OutboxEventStatus processing,
            @Param("pending") OutboxEventStatus pending,
            @Param("cutoff") Instant cutoff);
}