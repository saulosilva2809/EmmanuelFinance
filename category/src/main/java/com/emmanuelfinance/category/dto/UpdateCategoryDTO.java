package com.emmanuelfinance.category.dto;

import com.emmanuelfinance.shared.enums.TypeEnum;

import java.util.UUID;

public record UpdateCategoryDTO(
        UUID accountId,
        String name,
        TypeEnum type
) {}
