package com.viv.urlshortener.api;

import java.time.OffsetDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ShortUrlResponse", description = "Representation of a managed short URL.")
public record ShortUrlResponse(
        @Schema(example = "42")
        Long id,
        @Schema(example = "docs2026")
        String shortCode,
        @Schema(example = "http://localhost:8080/docs2026")
        String shortUrl,
        @Schema(example = "https://example.com/docs/spring-boot")
        String originalUrl,
        @Schema(example = "ACTIVE")
        String status,
        @Schema(example = "2026-03-28T08:15:30Z")
        OffsetDateTime createdAt,
        @Schema(example = "2026-12-31T23:59:59Z", nullable = true)
        OffsetDateTime expiresAt,
        @Schema(example = "2026-03-29T09:10:11Z", nullable = true)
        OffsetDateTime lastAccessedAt,
        @Schema(example = "12")
        long redirectCount
) {
}
