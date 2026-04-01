package com.viv.urlshortener.service.exception;

public class ShortUrlInactiveException extends RuntimeException {

    public ShortUrlInactiveException(String message) {
        super(message);
    }
}
