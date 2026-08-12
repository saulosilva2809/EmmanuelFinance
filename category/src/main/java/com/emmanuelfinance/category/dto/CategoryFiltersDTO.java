package com.emmanuelfinance.category.dto;

import com.emmanuelfinance.category.enums.TypeEnum;

import java.util.UUID;

public record CategoryFiltersDTO (
        UUID accountId,
        String name,
        TypeEnum type
) {}
