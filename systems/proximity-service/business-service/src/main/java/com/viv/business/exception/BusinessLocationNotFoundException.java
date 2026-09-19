package com.viv.business.exception;

public class BusinessLocationNotFoundException extends RuntimeException {

    public BusinessLocationNotFoundException() {
        super("Business location not found.");
    }

    public BusinessLocationNotFoundException(String message) {
        super(message);
    }

    public BusinessLocationNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public BusinessLocationNotFoundException(Throwable cause) {
        super(cause);
    }

}
