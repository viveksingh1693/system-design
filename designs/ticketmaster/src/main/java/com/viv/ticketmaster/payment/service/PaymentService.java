package com.viv.ticketmaster.payment.service;

import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.viv.ticketmaster.booking.entity.Booking;
import com.viv.ticketmaster.common.exception.PaymentFailedException;

@Service
public class PaymentService {

    public String authorizePayment(Booking booking, String paymentToken) {
        if (paymentToken == null || paymentToken.isBlank()) {
            throw new PaymentFailedException("Payment token is required to confirm booking " + booking.getId());
        }

        if (paymentToken.toLowerCase(Locale.ROOT).contains("fail")) {
            throw new PaymentFailedException("Payment authorization failed for booking " + booking.getId());
        }

        return "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }
}
