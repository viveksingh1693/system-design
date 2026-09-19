package com.viv.business.dto;


import java.time.Instant;
import java.util.List;

public record ApiErrorResponse(

        Instant timestamp,

        int status,

        String code,

        String message,

        String path,

        List<FieldErrorResponse> errors
) {
}