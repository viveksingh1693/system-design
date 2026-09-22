package com.viv.business.event;

import com.viv.business.entity.BusinessLocation;

public record LocationChange(
        BusinessLocation location,
        boolean created
) {}