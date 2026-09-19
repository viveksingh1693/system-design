package com.viv.business.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateBusinessRequest(

        @NotBlank
        @Size(max = 200)
        String name,

        @Size(max = 2000)
        String description,

        @Size(max = 20)
        List<@Valid UpdateBusinessLocationRequest> locations
) {
}