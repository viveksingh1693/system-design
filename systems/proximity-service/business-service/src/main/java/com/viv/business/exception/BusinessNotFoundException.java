package com.viv.business.exception;

// Custom exception for missing business category
public class BusinessNotFoundException extends RuntimeException {

    // Default constructor
    public BusinessNotFoundException() {
        super("Business not found.");
    }

    // Constructor with custom message
    public BusinessNotFoundException(String message) {
        super(message);
    }

    // Constructor with cause
    public BusinessNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    // Constructor with cause only
    public BusinessNotFoundException(Throwable cause) {
        super(cause);
    }
}

