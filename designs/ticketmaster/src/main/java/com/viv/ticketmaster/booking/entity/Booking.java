package com.viv.ticketmaster.booking.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.viv.ticketmaster.inventory.entity.Show;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "bookings")
@Getter
@Setter
@Schema(description = "Booking or hold created against a show.")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Primary key", example = "5001")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "show_id", nullable = false)
    private Show show;

    @Schema(description = "Customer email", example = "fan@example.com")
    private String customerEmail;

    @Schema(description = "Number of seats in the booking", example = "4")
    private int seatCount;

    @Schema(description = "Computed booking amount", example = "19996.00")
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Schema(description = "Lifecycle state of the booking", example = "HELD")
    private BookingStatus status;

    @Schema(description = "Hold expiry time for an unconfirmed booking", example = "2026-06-01T19:10:00")
    private LocalDateTime holdExpiresAt;

    @Schema(description = "Payment reference for confirmed bookings", example = "PAY-AB12CD34")
    private String paymentReference;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
