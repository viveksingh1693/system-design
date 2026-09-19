package com.viv.business.exception;

// Custom exception for missing business category
public class BusinessCategoryAlreadyExistsException extends RuntimeException {

    // Default constructor
    public BusinessCategoryAlreadyExistsException() {
        super("Business category already exists.");
    }

    // Constructor with custom message
    public BusinessCategoryAlreadyExistsException(String message) {
        super(message);
    }

    // Constructor with cause
    public BusinessCategoryAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }

    // Constructor with cause only
    public BusinessCategoryAlreadyExistsException(Throwable cause) {
        super(cause);
    }
}

