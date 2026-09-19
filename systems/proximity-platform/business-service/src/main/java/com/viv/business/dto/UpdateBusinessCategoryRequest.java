package com.viv.business.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateBusinessCategoryRequest(

        @NotBlank @Size(max = 100) String name,

        @Size(max = 500) String description) {
}