package com.viv.business.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;
import java.util.UUID;

public record CreateBusinessRequest(

        @NotBlank
        @Size(max = 200)
        String name,

        @Size(max = 2000)
        String description,

        @NotNull
        UUID categoryId,

        @NotEmpty
        @Size(max = 20)
        List<@Valid CreateBusinessLocationRequest> locations
) {
}