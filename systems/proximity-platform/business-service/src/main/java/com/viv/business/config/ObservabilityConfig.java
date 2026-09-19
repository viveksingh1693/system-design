package com.viv.business.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.micrometer.observation.ObservationRegistry;

@Configuration 
public class ObservabilityConfig {

    @Bean 
    ObservationRegistry observationRegistry() {
        return ObservationRegistry.create();
    }
}