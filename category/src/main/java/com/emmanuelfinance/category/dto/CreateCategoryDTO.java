package com.emmanuelfinance.category.dto;

import com.emmanuelfinance.category.enums.TypeEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateCategoryDTO(

        @NotNull(message = "The accountId is required")
        UUID accountId,

        @NotBlank(message = "The name is required")
        String name,

        @NotNull(message = "The type is required")
        TypeEnum type
) {}
