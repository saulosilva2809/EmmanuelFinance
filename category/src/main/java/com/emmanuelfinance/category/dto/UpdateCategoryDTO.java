package com.emmanuelfinance.category.dto;

import com.emmanuelfinance.shared.enums.TypeTransactionEnum;

public record UpdateCategoryDTO(
        String name,
        TypeTransactionEnum type
) {}
