package com.viv.applicationgateway.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.security")
public record EdgeSecurityProperties(
        @NotBlank String username,
        @NotBlank String password,
        @NotBlank String role
) {
}
