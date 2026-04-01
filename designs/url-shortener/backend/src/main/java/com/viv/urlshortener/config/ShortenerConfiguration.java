package com.viv.urlshortener.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({ShortenerProperties.class, CacheProperties.class})
public class ShortenerConfiguration {
}
