package com.viv.business.service;

import java.util.UUID;

import com.viv.business.dto.BusinessResponse;
import com.viv.business.dto.CreateBusinessRequest;
import com.viv.business.dto.UpdateBusinessRequest;

public interface BusinessService {

        BusinessResponse create(CreateBusinessRequest request);

        BusinessResponse getById(UUID id);

        BusinessResponse update(UUID id, UpdateBusinessRequest request);

        void deactivate(UUID id);

        void suspend(UUID id);

        void activate(UUID id);

        void deactivateLocation(UUID businessId, UUID locationId);

        
}