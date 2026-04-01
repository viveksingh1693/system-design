package com.viv.urlshortener.web;

import com.viv.urlshortener.api.CreateShortUrlRequest;
import com.viv.urlshortener.api.ErrorResponse;
import com.viv.urlshortener.api.ShortUrlResponse;
import com.viv.urlshortener.service.ShortUrlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/urls")
@Tag(name = "Short URLs", description = "Create, inspect, and disable managed short URLs.")
public class ShortUrlController {

    private final ShortUrlService shortUrlService;

    public ShortUrlController(ShortUrlService shortUrlService) {
        this.shortUrlService = shortUrlService;
    }

    @PostMapping
    @Operation(summary = "Create a short URL", description = "Creates a new short URL using either a generated code or a custom alias.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Short URL created",
                    content = @Content(schema = @Schema(implementation = ShortUrlResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Conflict while creating the short URL",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ShortUrlResponse> create(@Valid @RequestBody CreateShortUrlRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(shortUrlService.create(request));
    }

    @GetMapping("/{shortCode}")
    @Operation(summary = "Fetch short URL metadata", description = "Returns the stored details for a short code.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Short URL found",
                    content = @Content(schema = @Schema(implementation = ShortUrlResponse.class))),
            @ApiResponse(responseCode = "404", description = "Short code not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ShortUrlResponse getByCode(@PathVariable String shortCode) {
        return shortUrlService.getByCode(shortCode);
    }

    @DeleteMapping("/{shortCode}")
    @Operation(summary = "Disable a short URL", description = "Marks a short URL as disabled so future redirects are blocked.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Short URL disabled",
                    content = @Content(schema = @Schema(implementation = ShortUrlResponse.class))),
            @ApiResponse(responseCode = "404", description = "Short code not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ShortUrlResponse disable(@PathVariable String shortCode) {
        return shortUrlService.disable(shortCode);
    }

    @GetMapping("/health")
    @Operation(summary = "Application health probe", description = "Lightweight health check used by clients or infrastructure probes.")
    @ApiResponse(responseCode = "204", description = "Application is healthy")
    public ResponseEntity<Void> healthProbe() {
        return ResponseEntity.noContent().header(HttpHeaders.CACHE_CONTROL, "no-store").build();
    }
}
