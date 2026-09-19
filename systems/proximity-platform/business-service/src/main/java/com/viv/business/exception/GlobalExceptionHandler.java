package com.viv.business.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.viv.business.dto.ApiErrorResponse;
import com.viv.business.dto.FieldErrorResponse;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleBusinessNotFound(
            BusinessNotFoundException exception,
            HttpServletRequest request) {

        return build(
                HttpStatus.NOT_FOUND,
                "BUSINESS_NOT_FOUND",
                exception.getMessage(),
                request.getRequestURI(),
                List.of());
    }

    @ExceptionHandler(BusinessCategoryNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleCategoryNotFound(
            BusinessCategoryNotFoundException exception,
            HttpServletRequest request) {

        return build(
                HttpStatus.NOT_FOUND,
                "BUSINESS_CATEGORY_NOT_FOUND",
                exception.getMessage(),
                request.getRequestURI(),
                List.of());
    }

    @ExceptionHandler(BusinessLocationNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleLocationNotFound(
            BusinessLocationNotFoundException exception,
            HttpServletRequest request) {

        return build(
                HttpStatus.NOT_FOUND,
                "BUSINESS_LOCATION_NOT_FOUND",
                exception.getMessage(),
                request.getRequestURI(),
                List.of());
    }

    @ExceptionHandler(BusinessCategoryAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleCategoryAlreadyExists(
            BusinessCategoryAlreadyExistsException exception,
            HttpServletRequest request) {

        return build(
                HttpStatus.CONFLICT,
                "BUSINESS_CATEGORY_ALREADY_EXISTS",
                exception.getMessage(),
                request.getRequestURI(),
                List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {

        List<FieldErrorResponse> errors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new FieldErrorResponse(
                        error.getField(),
                        error.getDefaultMessage()))
                .toList();

        return build(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                "Request validation failed",
                request.getRequestURI(),
                errors);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(
            Exception exception,
            HttpServletRequest request) {

        return build(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_SERVER_ERROR",
                "An unexpected error occurred",
                request.getRequestURI(),
                List.of());
    }

    private ResponseEntity<ApiErrorResponse> build(
            HttpStatus status,
            String code,
            String message,
            String path,
            List<FieldErrorResponse> errors) {

        ApiErrorResponse response = new ApiErrorResponse(
                Instant.now(),
                status.value(),
                code,
                message,
                path,
                errors);

        return ResponseEntity
                .status(status)
                .body(response);
    }
}