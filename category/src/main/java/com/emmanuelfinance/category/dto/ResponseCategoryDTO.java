package com.emmanuelfinance.category.dto;

import com.emmanuelfinance.shared.enums.TypeTransactionEnum;

import java.time.LocalDateTime;
import java.util.UUID;

public record ResponseCategoryDTO(
        UUID id,
        String name,
        TypeTransactionEnum type,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean deleted
) {}
