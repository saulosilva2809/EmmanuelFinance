package com.emmanuelfinance.category.dto;

import com.emmanuelfinance.shared.enums.TypeEnum;

import java.util.UUID;

public record UpdateCategoryDTO(
        String name,
        TypeEnum type
) {}
