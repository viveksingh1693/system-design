package com.viv.ticketmaster.common.exception;

public class BookingExpiredException extends RuntimeException {

    public BookingExpiredException(String message) {
        super(message);
    }
}
