package com.emmanuelfinance.category.dto;

import com.emmanuelfinance.shared.enums.TypeEnum;

import java.time.LocalDateTime;
import java.util.UUID;

public record ResponseCategoryDTO(
        UUID id,
        String name,
        TypeEnum type,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean deleted
) {}
