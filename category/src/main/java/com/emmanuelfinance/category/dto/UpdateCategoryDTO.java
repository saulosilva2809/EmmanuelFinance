package com.emmanuelfinance.category.dto;

import com.emmanuelfinance.category.enums.TypeEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record UpdateCategoryDTO(
        UUID accountId,
        String name,
        TypeEnum type
) {}
