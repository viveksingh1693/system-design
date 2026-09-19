package com.viv.business.exception;

// Custom exception for missing business category
public class BusinessCategoryNotFoundException extends RuntimeException {

    // Default constructor
    public BusinessCategoryNotFoundException() {
        super("Business category not found.");
    }

    // Constructor with custom message
    public BusinessCategoryNotFoundException(String message) {
        super(message);
    }

    // Constructor with cause
    public BusinessCategoryNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    // Constructor with cause only
    public BusinessCategoryNotFoundException(Throwable cause) {
        super(cause);
    }
}

