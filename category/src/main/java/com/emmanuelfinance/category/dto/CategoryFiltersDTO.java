package com.emmanuelfinance.category.dto;

import com.emmanuelfinance.shared.enums.TypeTransactionEnum;

public record CategoryFiltersDTO (
        String name,
        TypeTransactionEnum type
) {}
