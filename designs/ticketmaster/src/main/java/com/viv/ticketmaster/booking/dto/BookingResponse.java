package com.viv.ticketmaster.booking.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.viv.ticketmaster.booking.entity.Booking;

public record BookingResponse(
        Long id,
        Long showId,
        String eventTitle,
        LocalDateTime startTime,
        String customerEmail,
        int seatCount,
        BigDecimal totalAmount,
        String status,
        LocalDateTime holdExpiresAt,
        String paymentReference
) {

    public static BookingResponse from(Booking booking) {
        return new BookingResponse(
                booking.getId(),
                booking.getShow().getId(),
                booking.getShow().getEvent().getTitle(),
                booking.getShow().getStartTime(),
                booking.getCustomerEmail(),
                booking.getSeatCount(),
                booking.getTotalAmount(),
                booking.getStatus().name(),
                booking.getHoldExpiresAt(),
                booking.getPaymentReference()
        );
    }
}
