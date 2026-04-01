package com.viv.urlshortener.api;

import java.time.OffsetDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ErrorResponse", description = "Error payload returned for failed API requests.")
public record ErrorResponse(
        @Schema(example = "2026-03-28T08:15:30Z")
        OffsetDateTime timestamp,
        @Schema(example = "409")
        int status,
        @Schema(example = "Conflict")
        String error,
        @Schema(example = "customAlias is already in use")
        String message,
        @Schema(example = "/api/v1/urls/docs2026")
        String path,
        @Schema(example = "[\"customAlias: already exists\"]")
        List<String> details
) {
}
