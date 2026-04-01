package com.viv.urlshortener.api;

import java.time.OffsetDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(name = "CreateShortUrlRequest", description = "Request payload used to create a new short URL.")
public record CreateShortUrlRequest(
        @Schema(
                description = "Destination URL that the short code redirects to.",
                example = "https://example.com/docs/spring-boot"
        )
        @NotBlank
        @Pattern(regexp = "^(https?)://.+$", message = "originalUrl must be a valid http or https URL")
        @Size(max = 2048)
        String originalUrl,

        @Schema(
                description = "Optional custom alias instead of a generated short code.",
                example = "docs2026",
                nullable = true
        )
        @Pattern(regexp = "^[a-zA-Z0-9_-]{4,32}$", message = "customAlias must be 4-32 characters and use only letters, numbers, '_' or '-'")
        String customAlias,

        @Schema(
                description = "Optional expiration timestamp for the short URL.",
                example = "2026-12-31T23:59:59Z",
                nullable = true
        )
        OffsetDateTime expiresAt
) {
}
