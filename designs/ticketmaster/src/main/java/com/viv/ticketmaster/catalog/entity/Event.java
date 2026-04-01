package com.viv.ticketmaster.catalog.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "events")
@Getter
@Setter
@Schema(description = "High-level event definition for discovery and onboarding.")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Primary key", example = "1")
    private Long id;

    @Schema(description = "Display title", example = "Coldplay Music of the Spheres")
    private String title;

    @Schema(description = "Primary artist or performer", example = "Coldplay")
    private String performer;

    @Schema(description = "City where the event is held", example = "Bengaluru")
    private String city;

    @Schema(description = "Venue name", example = "Chinnaswamy Stadium")
    private String venueName;

    @Schema(description = "Category of the event", example = "Concert")
    private String category;
}
