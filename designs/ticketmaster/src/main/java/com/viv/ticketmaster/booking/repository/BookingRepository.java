package com.viv.ticketmaster.booking.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.viv.ticketmaster.booking.entity.Booking;
import com.viv.ticketmaster.booking.entity.BookingStatus;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Query("select b from Booking b join fetch b.show s join fetch s.event where b.id = :bookingId")
    Optional<Booking> findDetailedById(@Param("bookingId") Long bookingId);

    List<Booking> findAllByShowIdAndStatusAndHoldExpiresAtBefore(
            Long showId,
            BookingStatus status,
            LocalDateTime before
    );
}
