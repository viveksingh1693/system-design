package com.viv.urlshortener.service;

import com.viv.urlshortener.api.ShortUrlResponse;
import com.viv.urlshortener.config.ShortenerProperties;
import com.viv.urlshortener.model.ShortUrl;
import org.springframework.stereotype.Component;

@Component
public class ShortUrlMapper {

    private final ShortenerProperties properties;

    public ShortUrlMapper(ShortenerProperties properties) {
        this.properties = properties;
    }

    public ShortUrlResponse toResponse(ShortUrl shortUrl) {
        return new ShortUrlResponse(
                shortUrl.getId(),
                shortUrl.getShortCode(),
                properties.baseUrl() + "/" + shortUrl.getShortCode(),
                shortUrl.getOriginalUrl(),
                shortUrl.getStatus().name(),
                shortUrl.getCreatedAt(),
                shortUrl.getExpiresAt(),
                shortUrl.getLastAccessedAt(),
                shortUrl.getRedirectCount()
        );
    }
}
