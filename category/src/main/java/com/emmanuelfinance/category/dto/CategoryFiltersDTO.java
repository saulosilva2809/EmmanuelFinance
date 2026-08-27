package com.emmanuelfinance.category.dto;

import com.emmanuelfinance.shared.enums.TypeEnum;

import java.util.UUID;

public record CategoryFiltersDTO (
        String name,
        TypeEnum type
) {}
