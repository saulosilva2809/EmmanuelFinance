package com.emmanuelfinance.category.dto;

import com.emmanuelfinance.shared.enums.TypeTransactionEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateCategoryDTO(

        @NotBlank(message = "The name is required")
        String name,

        @NotNull(message = "The type is required")
        TypeTransactionEnum type
) {}
