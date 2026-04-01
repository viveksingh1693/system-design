package com.viv.urlshortener.service.exception;

public class ShortUrlExpiredException extends RuntimeException {

    public ShortUrlExpiredException(String message) {
        super(message);
    }
}
