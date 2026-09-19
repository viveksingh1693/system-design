package com.viv.business.service;

import java.util.List;
import java.util.UUID;

import com.viv.business.dto.BusinessLocationResponse;
import com.viv.business.dto.CreateBusinessLocationRequest;
import com.viv.business.dto.UpdateBusinessLocationRequest;

public interface BusinessLocationService {

    BusinessLocationResponse create(
            UUID businessId,
            CreateBusinessLocationRequest request);

    List<BusinessLocationResponse> getByBusinessId(
            UUID businessId);

    BusinessLocationResponse update(
            UUID businessId,
            UUID locationId,
            UpdateBusinessLocationRequest request);

    void deactivate(
            UUID businessId,
            UUID locationId);
}