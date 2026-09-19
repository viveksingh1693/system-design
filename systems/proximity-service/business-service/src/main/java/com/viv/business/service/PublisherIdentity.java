package com.viv.business.service;

import java.util.UUID;

import org.springframework.stereotype.Component;

@Component 
public class PublisherIdentity {

    private final String id =
            UUID.randomUUID().toString();

    public String getId() {
        return id;
    }
}