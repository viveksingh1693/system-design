package com.viv.ticketmaster.booking.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.viv.ticketmaster.booking.dto.ConfirmBookingRequest;
import com.viv.ticketmaster.booking.dto.HoldBookingRequest;
import com.viv.ticketmaster.booking.entity.Booking;
import com.viv.ticketmaster.booking.entity.BookingStatus;
import com.viv.ticketmaster.booking.repository.BookingRepository;
import com.viv.ticketmaster.common.exception.BookingExpiredException;
import com.viv.ticketmaster.common.exception.ResourceNotFoundException;
import com.viv.ticketmaster.inventory.entity.Show;
import com.viv.ticketmaster.inventory.service.InventoryService;
import com.viv.ticketmaster.payment.service.PaymentService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final InventoryService inventoryService;
    private final PaymentService paymentService;

    @Value("${ticketmaster.booking.hold-duration-minutes:10}")
    private long holdDurationMinutes;


    @Transactional
    public Booking createHold(HoldBookingRequest request) {
        expireBookingsForShow(request.showId());

        Show show = inventoryService.reserveSeats(request.showId(), request.seatCount());
        LocalDateTime now = LocalDateTime.now();

        Booking booking = new Booking();
        booking.setShow(show);
        booking.setCustomerEmail(request.customerEmail());
        booking.setSeatCount(request.seatCount());
        booking.setTotalAmount(show.getPrice().multiply(BigDecimal.valueOf(request.seatCount())));
        booking.setStatus(BookingStatus.HELD);
        booking.setHoldExpiresAt(now.plusMinutes(holdDurationMinutes));
        booking.setCreatedAt(now);
        booking.setUpdatedAt(now);
        return bookingRepository.save(booking);
    }

    @Transactional
    public Booking confirmBooking(Long bookingId, ConfirmBookingRequest request) {
        Booking booking = getManagedBooking(bookingId);
        expireBookingIfNeeded(booking);

        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            return booking;
        }

        if (booking.getStatus() != BookingStatus.HELD) {
            throw new IllegalStateException("Only held bookings can be confirmed");
        }

        String paymentReference = paymentService.authorizePayment(booking, request.paymentToken());
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setPaymentReference(paymentReference);
        booking.setHoldExpiresAt(null);
        booking.setUpdatedAt(LocalDateTime.now());
        return booking;
    }

    @Transactional
    public Booking cancelBooking(Long bookingId) {
        Booking booking = getManagedBooking(bookingId);
        expireBookingIfNeeded(booking);

        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.EXPIRED) {
            return booking;
        }

        inventoryService.releaseSeats(booking.getShow().getId(), booking.getSeatCount());
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setUpdatedAt(LocalDateTime.now());
        booking.setHoldExpiresAt(null);
        return booking;
    }

    @Transactional
    public Booking getBooking(Long bookingId) {
        Booking booking = getManagedBooking(bookingId);
        expireBookingIfNeeded(booking);
        return booking;
    }

    private Booking getManagedBooking(Long bookingId) {
        return bookingRepository.findDetailedById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking " + bookingId + " was not found"));
    }

    private void expireBookingsForShow(Long showId) {
        LocalDateTime now = LocalDateTime.now();
        List<Booking> expiredHolds = bookingRepository.findAllByShowIdAndStatusAndHoldExpiresAtBefore(
                showId,
                BookingStatus.HELD,
                now
        );

        for (Booking booking : expiredHolds) {
            inventoryService.releaseSeats(showId, booking.getSeatCount());
            booking.setStatus(BookingStatus.EXPIRED);
            booking.setUpdatedAt(now);
        }
    }

    private void expireBookingIfNeeded(Booking booking) {
        if (booking.getStatus() != BookingStatus.HELD) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        if (booking.getHoldExpiresAt() != null && booking.getHoldExpiresAt().isBefore(now)) {
            inventoryService.releaseSeats(booking.getShow().getId(), booking.getSeatCount());
            booking.setStatus(BookingStatus.EXPIRED);
            booking.setUpdatedAt(now);
            throw new BookingExpiredException("Booking " + booking.getId() + " has expired");
        }
    }
}
