package com.emmanuelfinance.category.dto;

import com.emmanuelfinance.shared.enums.TypeEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateCategoryDTO(

        @NotBlank(message = "The name is required")
        String name,

        @NotNull(message = "The type is required")
        TypeEnum type
) {}
