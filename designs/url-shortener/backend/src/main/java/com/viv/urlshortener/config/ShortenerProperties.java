package com.viv.urlshortener.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.shortener")
public record ShortenerProperties(
        @NotBlank String baseUrl,
        int defaultCodeLength,
        int maxGenerationAttempts
) {
}
