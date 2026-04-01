package com.viv.urlshortener.service;

import java.net.URI;
import java.time.OffsetDateTime;

import com.viv.urlshortener.api.CreateShortUrlRequest;
import com.viv.urlshortener.api.ShortUrlResponse;
import com.viv.urlshortener.config.RedisCacheConfiguration;
import com.viv.urlshortener.config.ShortenerProperties;
import com.viv.urlshortener.model.ShortUrl;
import com.viv.urlshortener.model.ShortUrlStatus;
import com.viv.urlshortener.repository.ShortUrlRepository;
import com.viv.urlshortener.service.exception.ConflictException;
import com.viv.urlshortener.service.exception.ResourceNotFoundException;
import com.viv.urlshortener.service.exception.ShortUrlExpiredException;
import com.viv.urlshortener.service.exception.ShortUrlInactiveException;
import io.micrometer.observation.annotation.Observed;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ShortUrlService {

    private static final Logger log = LoggerFactory.getLogger(ShortUrlService.class);

    private final ShortUrlRepository repository;
    private final ShortCodeGenerator shortCodeGenerator;
    private final ShortUrlMapper mapper;
    private final ShortenerProperties properties;

    public ShortUrlService(
            ShortUrlRepository repository,
            ShortCodeGenerator shortCodeGenerator,
            ShortUrlMapper mapper,
            ShortenerProperties properties
    ) {
        this.repository = repository;
        this.shortCodeGenerator = shortCodeGenerator;
        this.mapper = mapper;
        this.properties = properties;
    }

    @Observed(name = "shortener.create")
    @Transactional
    @CachePut(cacheNames = RedisCacheConfiguration.SHORT_URL_CACHE, key = "#result.shortCode")
    public ShortUrlResponse create(CreateShortUrlRequest request) {
        OffsetDateTime now = OffsetDateTime.now();
        validateExpiration(request.expiresAt(), now);

        ShortUrl shortUrl = new ShortUrl();
        shortUrl.setOriginalUrl(normalizeUrl(request.originalUrl()));
        shortUrl.setShortCode(resolveShortCode(request.customAlias()));
        shortUrl.setExpiresAt(request.expiresAt());

        ShortUrl saved = repository.save(shortUrl);
        log.info("Created short URL with code={} targetHost={}", saved.getShortCode(), URI.create(saved.getOriginalUrl()).getHost());
        return mapper.toResponse(saved);
    }

    @Observed(name = "shortener.find")
    @Transactional
    @Cacheable(cacheNames = RedisCacheConfiguration.SHORT_URL_CACHE, key = "#shortCode")
    public ShortUrlResponse getByCode(String shortCode) {
        return mapper.toResponse(findByCode(shortCode));
    }

    @Observed(name = "shortener.redirect")
    @Transactional
    @CacheEvict(cacheNames = RedisCacheConfiguration.SHORT_URL_CACHE, key = "#shortCode")
    public String resolveRedirectUrl(String shortCode) {
        ShortUrl shortUrl = findByCode(shortCode);
        OffsetDateTime now = OffsetDateTime.now();

        if (shortUrl.getStatus() != ShortUrlStatus.ACTIVE) {
            throw new ShortUrlInactiveException("Short URL is not active");
        }
        if (shortUrl.isExpired(now)) {
            throw new ShortUrlExpiredException("Short URL has expired");
        }

        shortUrl.incrementRedirectCount();
        shortUrl.setLastAccessedAt(now);

        log.info("Redirect resolved for code={} redirectCount={}", shortCode, shortUrl.getRedirectCount());
        return shortUrl.getOriginalUrl();
    }

    @Observed(name = "shortener.disable")
    @Transactional
    @CachePut(cacheNames = RedisCacheConfiguration.SHORT_URL_CACHE, key = "#shortCode")
    public ShortUrlResponse disable(String shortCode) {
        ShortUrl shortUrl = findByCode(shortCode);
        shortUrl.setStatus(ShortUrlStatus.DISABLED);
        log.info("Disabled short URL with code={}", shortCode);
        return mapper.toResponse(shortUrl);
    }

    private ShortUrl findByCode(String shortCode) {
        return repository.findByShortCode(shortCode)
                .orElseThrow(() -> new ResourceNotFoundException("Short URL not found for code: " + shortCode));
    }

    private void validateExpiration(OffsetDateTime expiresAt, OffsetDateTime now) {
        if (expiresAt != null && !expiresAt.isAfter(now)) {
            throw new ConflictException("expiresAt must be a future timestamp");
        }
    }

    private String resolveShortCode(String customAlias) {
        if (StringUtils.hasText(customAlias)) {
            if (repository.existsByShortCode(customAlias)) {
                throw new ConflictException("customAlias is already in use");
            }
            return customAlias;
        }

        for (int attempt = 0; attempt < properties.maxGenerationAttempts(); attempt++) {
            String candidate = shortCodeGenerator.generate(properties.defaultCodeLength());
            if (!repository.existsByShortCode(candidate)) {
                return candidate;
            }
        }

        throw new ConflictException("Unable to allocate a unique short code");
    }

    private String normalizeUrl(String originalUrl) {
        return URI.create(originalUrl).normalize().toString();
    }
}
