package com.viv.business.repository;

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

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxEventRepository
                extends JpaRepository<OutboxEvent, UUID> {

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