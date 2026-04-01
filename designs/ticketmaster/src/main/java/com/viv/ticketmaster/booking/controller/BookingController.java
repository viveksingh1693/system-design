package com.viv.ticketmaster.booking.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.viv.ticketmaster.booking.dto.BookingResponse;
import com.viv.ticketmaster.booking.dto.ConfirmBookingRequest;
import com.viv.ticketmaster.booking.dto.HoldBookingRequest;
import com.viv.ticketmaster.booking.service.BookingService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
@Tag(name = "Bookings", description = "Ticket hold, confirmation, and cancellation APIs")
public class BookingController {

    private final BookingService bookingService;


    @PostMapping("/hold")
    @Operation(summary = "Hold seats", description = "Creates a temporary booking hold and decrements available inventory.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Seats held successfully",
                content = @Content(schema = @Schema(implementation = BookingResponse.class))),
        @ApiResponse(responseCode = "409", description = "Not enough seats available")
    })
    public BookingResponse holdSeats(@Valid @RequestBody HoldBookingRequest request) {
        return BookingResponse.from(bookingService.createHold(request));
    }

    @PostMapping("/{bookingId}/confirm")
    @Operation(summary = "Confirm booking", description = "Authorizes payment and converts a hold into a confirmed booking.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Booking confirmed",
                content = @Content(schema = @Schema(implementation = BookingResponse.class))),
        @ApiResponse(responseCode = "400", description = "Payment failed"),
        @ApiResponse(responseCode = "409", description = "Booking is expired or in an invalid state")
    })
    public BookingResponse confirmBooking(
            @PathVariable Long bookingId,
            @Valid @RequestBody ConfirmBookingRequest request
    ) {
        return BookingResponse.from(bookingService.confirmBooking(bookingId, request));
    }

    @PostMapping("/{bookingId}/cancel")
    @Operation(summary = "Cancel booking", description = "Cancels a held or confirmed booking and releases seats.")
    public BookingResponse cancelBooking(@PathVariable Long bookingId) {
        return BookingResponse.from(bookingService.cancelBooking(bookingId));
    }

    @GetMapping("/{bookingId}")
    @Operation(summary = "Get booking", description = "Returns the latest booking state, including expiry and payment details.")
    public BookingResponse getBooking(@PathVariable Long bookingId) {
        return BookingResponse.from(bookingService.getBooking(bookingId));
    }
}
