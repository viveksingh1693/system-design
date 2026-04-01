package com.viv.ticketmaster.inventory.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.viv.ticketmaster.inventory.entity.Show;

import jakarta.persistence.LockModeType;

public interface ShowRepository extends JpaRepository<Show, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Show s join fetch s.event where s.id = :showId")
    Optional<Show> findByIdForUpdate(@Param("showId") Long showId);

    @Query("""
            select s from Show s
            join fetch s.event e
            where (:city is null or lower(e.city) = lower(:city))
              and (
                    :query is null
                    or lower(e.title) like lower(concat('%', :query, '%'))
                    or lower(e.performer) like lower(concat('%', :query, '%'))
                  )
              and (:fromTime is null or s.startTime >= :fromTime)
              and (:toTime is null or s.startTime <= :toTime)
            order by s.startTime asc
            """)
    List<Show> search(
            @Param("city") String city,
            @Param("query") String query,
            @Param("fromTime") LocalDateTime fromTime,
            @Param("toTime") LocalDateTime toTime
    );

    @Query("select s from Show s join fetch s.event where s.id = :showId")
    Optional<Show> findDetailedById(@Param("showId") Long showId);
}
