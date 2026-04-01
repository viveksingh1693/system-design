package com.viv.ticketmaster.inventory.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.viv.ticketmaster.catalog.entity.Event;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "shows")
@Getter
@Setter
@Schema(description = "Scheduled performance with ticket inventory.")
public class Show {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Primary key", example = "101")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Schema(description = "Show start date and time", example = "2026-06-01T19:30:00")
    private LocalDateTime startTime;

    @Schema(description = "Ticket price", example = "4999.00")
    private BigDecimal price;

    @Schema(description = "Total sellable seats", example = "50000")
    private int totalSeats;

    @Schema(description = "Currently available seats", example = "49996")
    private int availableSeats;

    @Version
    private Long version;
}
